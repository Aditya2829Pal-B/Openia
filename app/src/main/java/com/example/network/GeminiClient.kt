package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY

    val isApiKeyAvailable: Boolean
        get() = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzePost(
        title: String,
        content: String,
        type: String, // "OPINION" or "PROBLEM"
        category: String
    ): Triple<String, String, String>? = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable) {
            Log.w(TAG, "Gemini API Key is not configured.")
            return@withContext getMockAnalysis(title, content, type, category)
        }

        val prompt = if (type == "PROBLEM") {
            """
            Analyze the following shared problem.
            Category: $category
            Title: $title
            Problem detail: $content

            Provide a response structured exactly with these three sections separated by '---':
            Section 1: Root Cause Analysis (Systemic, psychological, or situational factors)
            Section 2: Actionable Solutions & Workarounds (Immediate and long-term steps)
            Section 3: Empathetic Perspective & Consolation (Validation of feelings, supportive summary)
            """.trimIndent()
        } else {
            """
            Analyze the following shared opinion.
            Category: $category
            Title: $title
            Opinion detail: $content

            Provide a response structured exactly with these three sections separated by '---':
            Section 1: Core Arguments & Strengths (Why this opinion makes sense)
            Section 2: Blindspots & Counter-arguments (Constructive critiques or opposing viewpoints)
            Section 3: Global Context & Historical Precedent (How this opinion sits in broader historical or current world affairs)
            """.trimIndent()
        }

        try {
            // Build the standard REST JSON body manually for absolute safety and low complexity
            val requestBodyJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)
            }

            val requestBody = requestBodyJson.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Network request failed: ${response.code} ${response.message}")
                    return@withContext getMockAnalysis(title, content, type, category)
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
                            return@withContext parseThreeSections(text, type)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during analysis call", e)
        }

        return@withContext getMockAnalysis(title, content, type, category)
    }

    private fun parseThreeSections(text: String, type: String): Triple<String, String, String> {
        val parts = text.split("---")
        val fallbackType = if (type == "PROBLEM") "Problem Solutions" else "Alternative take"
        
        val sec1 = parts.getOrNull(0)?.trim()?.replace(Regex("^Section 1:?\\s*", RegexOption.IGNORE_CASE), "") ?: "Analysis unavailable"
        val sec2 = parts.getOrNull(1)?.trim()?.replace(Regex("^Section 2:?\\s*", RegexOption.IGNORE_CASE), "") ?: "Solutions unavailable"
        val sec3 = parts.getOrNull(2)?.trim()?.replace(Regex("^Section 3:?\\s*", RegexOption.IGNORE_CASE), "") ?: "Support unavailable"

        return Triple(sec1, sec2, sec3)
    }

    private fun getMockAnalysis(
        title: String,
        content: String,
        type: String,
        category: String
    ): Triple<String, String, String> {
        return if (type == "PROBLEM") {
            Triple(
                "Based on the problem under Category '$category', the core difficulty stems from institutional delays, mismatched expectations, or typical modern communication friction. Systematically, these issues multiply when transparency is low and feedback loops are delayed.",
                "1. Establish direct and clear boundary logs to track any occurrences.\n2. Engage in incremental solutions, checking in with your supports every 3-5 days.\n3. Create fallback mechanisms to reduce reliance on single choke-points.",
                "Your feelings of frustration are entirely valid. Navigating '$title' requires emotional reserves. Rest assured, many members have shared similar problems on Openia and found comfort in collective strategies."
            )
        } else {
            Triple(
                "This opinion on '$title' emphasizes modern efficiency, democratic feedback, and critical thinking. It speaks to a growing consensus that conventional approaches are no longer sufficient to solve dynamic problems.",
                "Opposing views point out that this perspective might overlook historical safety structures or marginalize group members who thrive under traditional, structured workflows.",
                "Historically, similar ideas gained major traction during periods of industrial or digital disruption. This fits in with the ongoing global debate between highly structured planning and prompt experimental action."
            )
        }
    }
}
