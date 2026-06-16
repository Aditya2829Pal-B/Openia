package com.example.data.realtime

import android.content.Context
import android.util.Log
import com.example.core.network.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

// Production systems would use a Room database entity for QueueItem
data class QueueItem(
    val id: String,
    val endpoint: String,
    val payload: String,
    var retryCount: Int = 0
)

class OfflineQueueManager(
    private val networkMonitor: NetworkMonitor,
    private val webSocketManager: WebSocketManager
) {
    private val queue = mutableListOf<QueueItem>()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            networkMonitor.isConnected.collectLatest { connected ->
                if (connected) {
                    processQueue()
                }
            }
        }
    }

    fun enqueue(endpoint: String, payload: JSONObject) {
        if (networkMonitor.isConnected.value) {
            // Just send immediately if connected
            Log.d("OfflineQueueManager", "Network connected, skipping queue for \$endpoint")
            // Send via retrofit or websockets depending on endpoint
            // Example for websocket:
            // webSocketManager.send(payload.toString())
        } else {
            Log.d("OfflineQueueManager", "Network disconnected. Queuing \$endpoint")
            queue.add(
                QueueItem(
                    id = java.util.UUID.randomUUID().toString(),
                    endpoint = endpoint,
                    payload = payload.toString()
                )
            )
        }
    }

    private fun processQueue() {
        if (queue.isEmpty()) return
        Log.d("OfflineQueueManager", "Processing \${queue.size} items in queue")
        
        val itemsToProcess = queue.toList()
        queue.clear()
        
        itemsToProcess.forEach { item ->
            // Re-attempt networking. If failure, enqueue back:
            // item.retryCount++
            // queue.add(item)
            Log.d("OfflineQueueManager", "Processed queued item: \${item.endpoint}")
        }
    }
}
