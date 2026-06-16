package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.local.AppDatabase
import com.example.data.repository.PostRepository
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PostViewModel
import com.example.ui.viewmodel.PostViewModelFactory

import com.example.data.realtime.WebSocketManager
import com.example.data.realtime.OfflineQueueManager
import com.example.data.realtime.SyncManager
import com.example.core.network.NetworkMonitor
import com.example.core.security.SessionManager

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = AppDatabase.getDatabase(applicationContext)
    val dao = database.postDao()
    val pendingOperationDao = database.pendingOperationDao()
    val repository = PostRepository(dao, pendingOperationDao)

    val sessionManager = SessionManager(applicationContext)
    val networkMonitor = NetworkMonitor(applicationContext)
    val webSocketManager = WebSocketManager(sessionManager)
    val offlineQueueManager = OfflineQueueManager(networkMonitor, webSocketManager)

    // Instantiate SyncManager as Single Source of Truth Background Synchronizer
    val syncManager = SyncManager(
        webSocketManager,
        offlineQueueManager,
        repository,
        networkMonitor,
        pendingOperationDao
    )
    syncManager.startSyncProcess()

    val viewModel: PostViewModel by viewModels {
      PostViewModelFactory(application, repository)
    }

    setContent {
      MyApplicationTheme {
        MainScreen(viewModel = viewModel)
      }
    }
  }
}
