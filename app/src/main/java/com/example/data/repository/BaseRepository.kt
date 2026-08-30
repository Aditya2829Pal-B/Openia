package com.example.data.repository

import com.example.core.network.ApiResult
import com.example.core.network.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Base Repository providing common functions for data fetching strategies.
 * Handles unified access patterns for Local (Room Database) and Remote (Gemini API) data sources.
 */
abstract class BaseRepository {

    /**
     * Executes a network call safely and maps it to an ApiResult.
     */
    protected suspend fun <T> safeNetworkCall(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        apiCall: suspend () -> T
    ): ApiResult<T> {
        return safeApiCall(dispatcher, apiCall)
    }

    /**
     * Executes a database call safely and maps it to an ApiResult.
     */
    protected suspend fun <T> safeDatabaseCall(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        dbCall: suspend () -> T
    ): ApiResult<T> {
        return withContext(dispatcher) {
            try {
                ApiResult.Success(dbCall())
            } catch (e: Exception) {
                ApiResult.Error(message = e.message ?: "Database error occurred", exception = e)
            }
        }
    }

    /**
     * NetworkBoundResource implementation.
     * Strategy:
     * 1. Emit loading state.
     * 2. Query the local database for cached data.
     * 3. Evaluate if we should fetch from remote API based on `shouldFetch` predicate.
     * 4. If true, attempt to fetch from network, save the result to the local DB, and re-query the DB.
     * 5. If false (or network fails), emit the local data.
     * 
     * This elegantly combines Room and Retrofit/Gemini data flows into a single source of truth.
     */
    protected inline fun <ResultType, RequestType> networkBoundResource(
        crossinline query: () -> Flow<ResultType>,
        crossinline fetch: suspend () -> RequestType,
        crossinline saveFetchResult: suspend (RequestType) -> Unit,
        crossinline shouldFetch: (ResultType) -> Boolean = { true },
        crossinline onFetchFailed: (Throwable) -> Unit = { },
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Flow<ApiResult<ResultType>> = flow {
        emit(ApiResult.Loading)
        
        val localData = query().first()
        
        if (shouldFetch(localData)) {
            emit(ApiResult.Loading)
            try {
                val remoteData = fetch()
                saveFetchResult(remoteData)
                emitAll(query().map { ApiResult.Success(it) })
            } catch (e: Throwable) {
                onFetchFailed(e)
                emitAll(query().map { ApiResult.Error(message = e.message ?: "Network error", exception = e) })
            }
        } else {
            emitAll(query().map { ApiResult.Success(it) })
        }
    }.catch { e ->
        emit(ApiResult.Error(message = e.message ?: "Unknown error", exception = e))
    }.flowOn(dispatcher)
}
