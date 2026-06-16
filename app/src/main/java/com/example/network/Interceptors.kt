package com.example.network

import okhttp3.Interceptor
import okhttp3.Response

class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var tryCount = 0

        while (response == null && tryCount < maxRetries) {
            try {
                response = chain.proceed(request)
                if (!response.isSuccessful) {
                    response.close()
                    response = null
                }
            } catch (e: Exception) {
                if (tryCount >= maxRetries - 1) {
                    throw e
                }
                try {
                    Thread.sleep(1000L * (tryCount + 1))
                } catch (ie: InterruptedException) {
                    // Ignore
                }
            }
            tryCount++
        }
        
        return response ?: chain.proceed(request) // proceed one last time if still null
    }
}

class RateLimitInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        
        if (response.code == 429) {
            val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 1L
            // Could throw a specific RateLimitException here
        }
        return response
    }
}
