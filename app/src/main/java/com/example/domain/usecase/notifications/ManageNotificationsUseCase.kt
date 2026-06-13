package com.example.domain.usecase.notifications

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class NotificationModel(
    val id: Int,
    val title: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean
)

class ManageNotificationsUseCase {

    fun getNotificationsFlow(): Flow<List<NotificationModel>> {
        // Return baseline structured notifications to satisfy clean architecture structures
        return flowOf(
            listOf(
                NotificationModel(
                    id = 1,
                    title = "Reputation Level Up!",
                    body = "You scored new validation on your suggestions. Check your profile badge!",
                    timestamp = System.currentTimeMillis() - 3600000,
                    isRead = false
                ),
                NotificationModel(
                    id = 2,
                    title = "EcoWarrior agreed with you",
                    body = "@EcoWarrior liked your sustainable thinking post category solution.",
                    timestamp = System.currentTimeMillis() - 7200000,
                    isRead = true
                )
            )
        )
    }

    suspend fun markAsRead(id: Int) {
        // Prepare for offline sync / Room update hook
    }
}
