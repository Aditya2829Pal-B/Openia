package com.example.data.realtime

import com.example.data.realtime.PendingOperationDao
import android.util.Log
import com.example.core.network.NetworkMonitor
import com.example.data.repository.PostRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SyncManager(
    private val webSocketManager: WebSocketManager,
    private val offlineQueueManager: OfflineQueueManager,
    private val postRepository: PostRepository,
    private val networkMonitor: NetworkMonitor,
    private val pendingOperationDao: PendingOperationDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            webSocketManager.events.collectLatest { event ->
                handleRealTimeEvent(event)
            }
        }
        
        scope.launch {
            networkMonitor.isConnected.collectLatest { connected ->
                if (connected) {
                    flushPendingQueue()
                }
            }
        }
    }

    private suspend fun handleRealTimeEvent(event: RealTimeEvent) {
        when (event) {
            is RealTimeEvent.PostUpdated -> {
                Log.d("SyncManager", "Post updated: \${event.postId}")
            }
            is RealTimeEvent.ReputationChanged -> {
                Log.d("SyncManager", "Reputation changed for: \${event.username} to \${event.points}")
            }
            is RealTimeEvent.TypingUpdate -> {
                Log.d("SyncManager", "Users typing on \${event.postId}: \${event.usersTyping}")
            }
            is RealTimeEvent.FollowerUpdate -> {
                Log.d("SyncManager", "Follower count updated for \${event.username}: \${event.followersCount}")
            }
        }
    }

    private suspend fun flushPendingQueue() {
        Log.d("SyncManager", "Network connected. Flushing pending queue...")
        val queue = pendingOperationDao.getAllPendingOperationsSync()
        for (op in queue) {
            try {
                // Execute network operation corresponding to `op.operationType` and `op.payloadJson`
                Log.d("SyncManager", "Executing operation: \${op.operationType}")
                
                // If successful:
                pendingOperationDao.deleteOperation(op)
            } catch (e: Exception) {
                Log.e("SyncManager", "Failed to sync operation \${op.id}", e)
                // Stop flushing, wait for next connection
                break
            }
        }
    }

    fun startSyncProcess() {
        Log.d("SyncManager", "Starting general sync process for missing changes")
        scope.launch {
            if (networkMonitor.isConnected.value) {
                flushPendingQueue()
            }
        }
    }
}
