package com.example.data.realtime

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class RealTimeEvent {
    data class PostUpdated(val postId: Int, val payload: JSONObject) : RealTimeEvent()
    data class ReputationChanged(val username: String, val points: Int, val rankLevel: Int) : RealTimeEvent()
    data class TypingUpdate(val postId: Int, val usersTyping: List<String>) : RealTimeEvent()
    data class FollowerUpdate(val username: String, val followersCount: Int) : RealTimeEvent()
}

class WebSocketManager(
    private val tokenStorage: com.example.core.security.SessionManager
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val _events = MutableSharedFlow<RealTimeEvent>(replay = 10)
    val events: SharedFlow<RealTimeEvent> = _events.asSharedFlow()

    fun connect(url: String) {
        val token = tokenStorage.getAuthToken() ?: return
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer \$token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.getString("type")) {
                        "postUpdated" -> {
                            val payload = json.getJSONObject("payload")
                            _events.tryEmit(RealTimeEvent.PostUpdated(payload.getInt("postId"), payload))
                        }
                        "reputationChanged" -> {
                            val payload = json.getJSONObject("payload")
                            _events.tryEmit(RealTimeEvent.ReputationChanged(
                                payload.getString("username"),
                                payload.getInt("points"),
                                payload.getInt("rankLevel")
                            ))
                        }
                        "typingUpdate" -> {
                            val payload = json.getJSONObject("payload")
                            val arr = payload.getJSONArray("usersTyping")
                            val users = mutableListOf<String>()
                            for (i in 0 until arr.length()) {
                                users.add(arr.getString(i))
                            }
                            _events.tryEmit(RealTimeEvent.TypingUpdate(payload.getInt("postId"), users))
                        }
                        "followerUpdate" -> {
                            val payload = json.getJSONObject("payload")
                            _events.tryEmit(RealTimeEvent.FollowerUpdate(
                                payload.getString("username"),
                                payload.getInt("followersCount")
                            ))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Parse error", e)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closed: \$reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Error: \${t.message}", t)
                // Implement exponential backoff reconnection here
            }
        })
    }

    fun subscribeToPost(postId: Int) {
        val payload = JSONObject().apply {
            put("event", "subscribeToPost")
            put("postId", postId)
        }
        webSocket?.send(payload.toString())
    }

    fun triggerTyping(postId: Int) {
        val payload = JSONObject().apply {
            put("event", "typingStart")
            put("postId", postId)
        }
        webSocket?.send(payload.toString())
    }
    
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }
}
