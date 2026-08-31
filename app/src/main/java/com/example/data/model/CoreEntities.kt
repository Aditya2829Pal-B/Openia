package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val displayName: String,
    val bio: String? = null,
    val avatarUri: String? = null,
    val isVerified: Boolean = false,
    val joinedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "discussions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["authorId"]), Index(value = ["category"])]
)
data class Discussion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorId: Int,
    val title: String,
    val content: String,
    val category: String,
    val tags: String,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reputation_points",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class ReputationPoint(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val points: Int,
    val source: String, // e.g., "POST_UPVOTE", "COMMENT_ACCEPTED"
    val description: String,
    val awardedAt: Long = System.currentTimeMillis()
)
