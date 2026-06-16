package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postType: String, // "OPINION" or "PROBLEM"
    val title: String,
    val content: String,
    val category: String,
    val tags: String, // Comma-separated tags, e.g., "tech,future,ethics"
    val author: String,
    val avatarSeed: String,
    val timestamp: Long = System.currentTimeMillis(),
    val agreeCount: Int = 0,
    val disagreeCount: Int = 0,
    val upvotesCount: Int = 0,
    val downvotesCount: Int = 0,
    val empathyCount: Int = 0,
    val commentCount: Int = 0,
    val aiSummary: String? = null,
    val aiSolutions: String? = null,
    val aiConsensus: String? = null,
    val parentPostId: Int? = null,
    val versionHistoryId: String? = null,
    val consensusCredibility: Float = 0.8f,
    val misinformationProbability: Float = 0.05f,
    val imageUri: String? = null
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val author: String,
    val avatarSeed: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSolution: Boolean = false,
    val upvotesCount: Int = 0,
    val downvotesCount: Int = 0,
    val parentCommentId: Int? = null
)

@Entity(tableName = "user_reactions", primaryKeys = ["postId", "reactionType"])
data class UserReactionEntity(
    val postId: Int,
    val reactionType: String // "AGREE", "DISAGREE", "UPVOTE", "DOWNVOTE", "EMPATHY"
)

@Entity(tableName = "comment_reactions", primaryKeys = ["commentId", "reactionType"])
data class CommentReactionEntity(
    val commentId: Int,
    val reactionType: String // "UPVOTE", "DOWNVOTE"
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val username: String = "You",
    val displayName: String = "Openian Citizen",
    val bio: String = "Sharing alternative insights and solutions on Openia.",
    val avatarSeed: String = "Y",
    val baseFollowers: Int = 12
)

@Entity(tableName = "follows")
data class FollowEntity(
    @PrimaryKey val followedAuthor: String
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val postId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: String,
    val type: String, // "OPINION" or "PROBLEM"
    val timestamp: Long = System.currentTimeMillis()
)
