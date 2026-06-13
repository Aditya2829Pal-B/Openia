package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.CommentEntity
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

data class ThreadAnalysisResult(
    val trendAnalysis: String,
    val sentimentSynthesis: String
)

class GeminiRepository {
    private val TAG = "GeminiRepository"
    private val MODEL_NAME = "gemini-3.5-flash"
    private val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/${MODEL_NAME}:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val isApiKeyAvailable: Boolean
        get() = BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    suspend fun analyzeDiscussionThread(
        post: PostEntity,
        comments: List<CommentEntity>
    ): ThreadAnalysisResult? = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable) {
            Log.w(TAG, "Gemini API Key is not configured.")
            return@withContext getMockAnalysis()
        }

        val threadContext = buildString {
            append("Post [${post.postType}] in ${post.category}: ${post.title}\n")
            append("Content: ${post.content}\n\n")
            append("Comments:\n")
            comments.forEach {
                append("- ${it.author}: ${it.content}\n")
            }
        }

        val prompt = """
            Analyze the following discussion thread:
            
            $threadContext
            
            Perform trend analysis and sentiment synthesis on this thread.
            Provide your response in exactly two sections separated by '|||':
            Section 1: Trend Analysis (Identify the overall trend, common themes, and key takeaways from the discussion)
            Section 2: Sentiment Synthesis (Summarize the general sentiment, varying perspectives, and emotional tone of the participants)
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
                    return@withContext getMockAnalysis()
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
                            return@withContext parseAnalysisResult(text)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during thread analysis", e)
        }

        return@withContext getMockAnalysis()
    }

    private fun parseAnalysisResult(text: String): ThreadAnalysisResult {
        val split = text.split("|||")
        val trendAnalysis = split.getOrNull(0)?.trim() ?: "Trend Analysis unavailable."
        val sentimentSynthesis = split.getOrNull(1)?.trim() ?: "Sentiment Synthesis unavailable."
        
        return ThreadAnalysisResult(
            trendAnalysis = trendAnalysis.replace(Regex("^Section 1:?\\s*", RegexOption.IGNORE_CASE), "").trim(),
            sentimentSynthesis = sentimentSynthesis.replace(Regex("^Section 2:?\\s*", RegexOption.IGNORE_CASE), "").trim()
        )
    }

    private fun getMockAnalysis(): ThreadAnalysisResult {
        return ThreadAnalysisResult(
            trendAnalysis = "The overall trend leans towards constructive debate, with dominant themes exploring alternative solutions and challenging the traditional narrative.",
            sentimentSynthesis = "The prevailing sentiment is cautious optimism mixed with skepticism. Various perspectives clash respectfully, reflecting a highly engaged and emotionally invested community."
        )
    }
}
