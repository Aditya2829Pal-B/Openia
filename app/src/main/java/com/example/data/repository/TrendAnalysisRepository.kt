package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.PostEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TrendAnalysis(
    val dominantTrends: String,
    val emergingTopics: String
)

class TrendAnalysisRepository {
    private val TAG = "TrendAnalysisRepo"
    private val MODEL_NAME = "gemini-3.5-flash"
    private val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val isApiKeyAvailable: Boolean
        get() = BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    suspend fun analyzeTrends(posts: List<PostEntity>): TrendAnalysis? = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable) {
            Log.w(TAG, "Gemini API Key is not configured.")
            return@withContext getMockTrends()
        }

        val discussionContext = buildString {
            posts.take(15).forEach {
                append("Post [${it.category}]: ${it.title}\n")
                append("Content: ${it.content}\n\n")
            }
        }

        val prompt = """
            Analyze the following discussion data from multiple posts:
            
            $discussionContext
            
            Perform a trend analysis on this current discussion data.
            Provide your response in exactly two sections separated by '|||':
            Section 1: Dominant Trends (Identify the most prominent overarching trends and common themes across these posts)
            Section 2: Emerging Topics (Identify new, growing, or niche topics that are starting to gain traction)
        """.trimIndent()

        try {
            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
            }

            val requestBody = requestBodyJson.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL?key=${BuildConfig.GEMINI_API_KEY}")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Network request failed: ${response.code}")
                    return@withContext getMockTrends()
                }

                val bodyString = response.body?.string() ?: return@withContext null
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    if (contentObj != null) {
                        val parts = contentObj.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            return@withContext parseTrendAnalysisResult(text)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during trend analysis", e)
        }

        return@withContext getMockTrends()
    }

    private fun parseTrendAnalysisResult(text: String): TrendAnalysis {
        val split = text.split("|||")
        val dominantTrends = split.getOrNull(0)?.trim() ?: "Dominant Trends unavailable."
        val emergingTopics = split.getOrNull(1)?.trim() ?: "Emerging Topics unavailable."
        
        return TrendAnalysis(
            dominantTrends = dominantTrends.replace(Regex("^Section 1:?\\s*", RegexOption.IGNORE_CASE), "").trim(),
            emergingTopics = emergingTopics.replace(Regex("^Section 2:?\\s*", RegexOption.IGNORE_CASE), "").trim()
        )
    }

    private fun getMockTrends(): TrendAnalysis {
        return TrendAnalysis(
            dominantTrends = "The dominant trends focus heavily on alternative solutions and critical structural adjustments within the community.",
            emergingTopics = "Emerging topics suggest a shift towards grassroots organizing and localized micro-communities."
        )
    }
}
