package com.example.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.data.local.DiscussionsDao
import com.example.data.model.CommentEntity
import com.example.data.model.PostEntity
import com.example.data.model.UserReactionEntity
import kotlinx.coroutines.flow.Flow

import com.example.data.model.UserProfileEntity
import com.example.data.model.FollowEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.DraftEntity

import com.example.data.realtime.PendingOperation
import com.example.data.realtime.PendingOperationDao
import org.json.JSONObject

class PostRepository(
    private val postDao: DiscussionsDao,
    private val pendingOperationDao: PendingOperationDao
) {

    val allPosts: Flow<List<PostEntity>> = postDao.getAllPostsFlow()
    
    fun getDynamicPagingSource(
        type: String,
        category: String,
        query: String,
        followedOnly: Boolean,
        follows: List<String>
    ): androidx.paging.PagingSource<Int, PostEntity> {
        return postDao.getDynamicPagingSource(type, category, query, followedOnly, follows)
    }

    val myProfile: Flow<UserProfileEntity?> = postDao.getUserProfileFlow("You")

    val allFollows: Flow<List<FollowEntity>> = postDao.getFollowsFlow()
    
    val allBookmarks: Flow<List<BookmarkEntity>> = postDao.getAllBookmarksFlow()
    
    val allDrafts: Flow<List<DraftEntity>> = postDao.getAllDraftsFlow()

    suspend fun toggleBookmark(postId: Int) {
        val existing = postDao.getBookmarkByPostId(postId)
        if (existing != null) {
            postDao.deleteBookmark(existing)
            enqueueOperation("REMOVE_BOOKMARK", JSONObject().apply { put("postId", postId) })
        } else {
            postDao.insertBookmark(BookmarkEntity(postId))
            enqueueOperation("ADD_BOOKMARK", JSONObject().apply { put("postId", postId) })
        }
    }
    
    suspend fun saveDraft(draft: DraftEntity) {
        postDao.insertDraft(draft)
        // Note: We might only want to enqueue once Draft is published
    }
    
    suspend fun deleteDraft(id: Int) {
        postDao.deleteDraftById(id)
    }

    suspend fun getProfileDirect(username: String): UserProfileEntity? = postDao.getUserProfile(username)

    private suspend fun enqueueOperation(type: String, payload: JSONObject) {
        pendingOperationDao.insertOperation(
            PendingOperation(
                operationType = type,
                payloadJson = payload.toString()
            )
        )
    }

    suspend fun updateProfile(profile: UserProfileEntity) {
        postDao.insertUserProfile(profile)
        enqueueOperation("UPDATE_PROFILE", JSONObject().apply {
            put("username", profile.username)
        })
    }

    suspend fun toggleFollow(authorName: String) {
        val existing = postDao.getFollowByAuthor(authorName)
        if (existing != null) {
            postDao.deleteFollow(existing)
            enqueueOperation("UNFOLLOW_USER", JSONObject().apply {
                put("author", authorName)
            })
        } else {
            postDao.insertFollow(FollowEntity(authorName))
            enqueueOperation("FOLLOW_USER", JSONObject().apply {
                put("author", authorName)
            })
        }
    }

    fun getPostById(id: Int): Flow<PostEntity?> = postDao.getPostByIdFlow(id)

    suspend fun getPostByIdDirect(id: Int): PostEntity? = postDao.getPostById(id)

    fun getCommentsForPost(postId: Int): Flow<List<CommentEntity>> = 
        postDao.getCommentsByPostFlow(postId)

    fun getUserReactionsForPost(postId: Int): Flow<List<UserReactionEntity>> =
        postDao.getUserReactionsForPostFlow(postId)

    suspend fun createPost(post: PostEntity): Long {
        val id = postDao.insertPost(post)
        enqueueOperation("CREATE_POST", JSONObject().apply {
            put("id", id)
            put("title", post.title)
        })
        return id
    }

    suspend fun deletePost(post: PostEntity) {
        postDao.deletePost(post)
        enqueueOperation("DELETE_POST", JSONObject().apply {
            put("id", post.id)
        })
    }

    suspend fun updatePost(post: PostEntity) {
        postDao.updatePost(post)
        enqueueOperation("UPDATE_POST", JSONObject().apply {
            put("id", post.id)
        })
    }

    suspend fun createComment(comment: CommentEntity) {
        postDao.insertComment(comment)
        // Also increment comment count on post
        val post = postDao.getPostById(comment.postId)
        if (post != null) {
            postDao.updatePost(post.copy(commentCount = post.commentCount + 1))
        }
        enqueueOperation("CREATE_COMMENT", JSONObject().apply {
            put("postId", comment.postId)
            put("content", comment.content)
        })
    }

    suspend fun deleteComment(comment: CommentEntity) {
        postDao.deleteComment(comment)
        val post = postDao.getPostById(comment.postId)
        if (post != null) {
            postDao.updatePost(post.copy(commentCount = maxOf(0, post.commentCount - 1)))
        }
        enqueueOperation("DELETE_COMMENT", JSONObject().apply {
            put("commentId", comment.id)
        })
    }

    suspend fun toggleAgree(postId: Int) {
        val post = postDao.getPostById(postId) ?: return
        val existingAgree = postDao.getSpecificReaction(postId, "AGREE")
        val existingDisagree = postDao.getSpecificReaction(postId, "DISAGREE")

        var newAgreeCount = post.agreeCount
        var newDisagreeCount = post.disagreeCount

        if (existingAgree != null) {
            // Untoggle AGREE
            postDao.deleteReaction(existingAgree)
            newAgreeCount = maxOf(0, newAgreeCount - 1)
        } else {
            // Remove DISAGREE if exists
            if (existingDisagree != null) {
                postDao.deleteReaction(existingDisagree)
                newDisagreeCount = maxOf(0, newDisagreeCount - 1)
            }
            // Add AGREE
            postDao.insertReaction(UserReactionEntity(postId, "AGREE"))
            newAgreeCount++
        }

        postDao.updatePost(
            post.copy(
                agreeCount = newAgreeCount,
                disagreeCount = newDisagreeCount
            )
        )
        
        enqueueOperation("TOGGLE_REACTION", JSONObject().apply {
            put("postId", postId)
            put("reactionType", "AGREE")
        })
    }

    suspend fun toggleDisagree(postId: Int) {
        val post = postDao.getPostById(postId) ?: return
        val existingAgree = postDao.getSpecificReaction(postId, "AGREE")
        val existingDisagree = postDao.getSpecificReaction(postId, "DISAGREE")

        var newAgreeCount = post.agreeCount
        var newDisagreeCount = post.disagreeCount

        if (existingDisagree != null) {
            // Untoggle DISAGREE
            postDao.deleteReaction(existingDisagree)
            newDisagreeCount = maxOf(0, newDisagreeCount - 1)
        } else {
            // Remove AGREE if exists
            if (existingAgree != null) {
                postDao.deleteReaction(existingAgree)
                newAgreeCount = maxOf(0, newAgreeCount - 1)
            }
            // Add DISAGREE
            postDao.insertReaction(UserReactionEntity(postId, "DISAGREE"))
            newDisagreeCount++
        }

        postDao.updatePost(
            post.copy(
                agreeCount = newAgreeCount,
                disagreeCount = newDisagreeCount
            )
        )

        enqueueOperation("TOGGLE_REACTION", JSONObject().apply {
            put("postId", postId)
            put("reactionType", "DISAGREE")
        })
    }

    suspend fun toggleUpvote(postId: Int) {
        val post = postDao.getPostById(postId) ?: return
        val existingUp = postDao.getSpecificReaction(postId, "UPVOTE")
        val existingDown = postDao.getSpecificReaction(postId, "DOWNVOTE")

        var newUpCount = post.upvotesCount
        var newDownCount = post.downvotesCount

        if (existingUp != null) {
            postDao.deleteReaction(existingUp)
            newUpCount = maxOf(0, newUpCount - 1)
        } else {
            if (existingDown != null) {
                postDao.deleteReaction(existingDown)
                newDownCount = maxOf(0, newDownCount - 1)
            }
            postDao.insertReaction(UserReactionEntity(postId, "UPVOTE"))
            newUpCount++
        }

        postDao.updatePost(
            post.copy(
                upvotesCount = newUpCount,
                downvotesCount = newDownCount
            )
        )
        
        enqueueOperation("TOGGLE_REACTION", JSONObject().apply {
            put("postId", postId)
            put("reactionType", "UPVOTE")
        })
    }

    suspend fun toggleDownvote(postId: Int) {
        val post = postDao.getPostById(postId) ?: return
        val existingUp = postDao.getSpecificReaction(postId, "UPVOTE")
        val existingDown = postDao.getSpecificReaction(postId, "DOWNVOTE")

        var newUpCount = post.upvotesCount
        var newDownCount = post.downvotesCount

        if (existingDown != null) {
            postDao.deleteReaction(existingDown)
            newDownCount = maxOf(0, newDownCount - 1)
        } else {
            if (existingUp != null) {
                postDao.deleteReaction(existingUp)
                newUpCount = maxOf(0, newUpCount - 1)
            }
            postDao.insertReaction(UserReactionEntity(postId, "DOWNVOTE"))
            newDownCount++
        }

        postDao.updatePost(
            post.copy(
                upvotesCount = newUpCount,
                downvotesCount = newDownCount
            )
        )
        
        enqueueOperation("TOGGLE_REACTION", JSONObject().apply {
            put("postId", postId)
            put("reactionType", "DOWNVOTE")
        })
    }

    suspend fun toggleEmpathy(postId: Int) {
        val post = postDao.getPostById(postId) ?: return
        val existingEmpathy = postDao.getSpecificReaction(postId, "EMPATHY")

        var newEmpathyCount = post.empathyCount

        if (existingEmpathy != null) {
            postDao.deleteReaction(existingEmpathy)
            newEmpathyCount = maxOf(0, newEmpathyCount - 1)
        } else {
            postDao.insertReaction(UserReactionEntity(postId, "EMPATHY"))
            newEmpathyCount++
        }

        postDao.updatePost(post.copy(empathyCount = newEmpathyCount))
        
        enqueueOperation("TOGGLE_REACTION", JSONObject().apply {
            put("postId", postId)
            put("reactionType", "EMPATHY")
        })
    }

    suspend fun saveAiAnalysis(postId: Int, summary: String?, solutions: String?, consensus: String?) {
        val post = postDao.getPostById(postId) ?: return
        postDao.updatePost(
            post.copy(
                aiSummary = summary,
                aiSolutions = solutions,
                aiConsensus = consensus
            )
        )
    }

    fun getCommentReactions(commentId: Int): Flow<List<com.example.data.model.CommentReactionEntity>> =
        postDao.getReactionsForCommentFlow(commentId)

    suspend fun toggleCommentUpvote(commentId: Int) {
        val comment = postDao.getCommentById(commentId) ?: return
        val existingUp = postDao.getSpecificCommentReaction(commentId, "UPVOTE")
        val existingDown = postDao.getSpecificCommentReaction(commentId, "DOWNVOTE")

        var newUpCount = comment.upvotesCount
        var newDownCount = comment.downvotesCount

        if (existingUp != null) {
            postDao.deleteCommentReaction(existingUp)
            newUpCount = maxOf(0, newUpCount - 1)
        } else {
            if (existingDown != null) {
                postDao.deleteCommentReaction(existingDown)
                newDownCount = maxOf(0, newDownCount - 1)
            }
            postDao.insertCommentReaction(com.example.data.model.CommentReactionEntity(commentId, "UPVOTE"))
            newUpCount++
        }

        postDao.updateComment(
            comment.copy(
                upvotesCount = newUpCount,
                downvotesCount = newDownCount
            )
        )
        
        enqueueOperation("TOGGLE_COMMENT_REACTION", JSONObject().apply {
            put("commentId", commentId)
            put("reactionType", "UPVOTE")
        })
    }

    suspend fun toggleCommentDownvote(commentId: Int) {
        val comment = postDao.getCommentById(commentId) ?: return
        val existingUp = postDao.getSpecificCommentReaction(commentId, "UPVOTE")
        val existingDown = postDao.getSpecificCommentReaction(commentId, "DOWNVOTE")

        var newUpCount = comment.upvotesCount
        var newDownCount = comment.downvotesCount

        if (existingDown != null) {
            postDao.deleteCommentReaction(existingDown)
            newDownCount = maxOf(0, newDownCount - 1)
        } else {
            if (existingUp != null) {
                postDao.deleteCommentReaction(existingUp)
                newUpCount = maxOf(0, newUpCount - 1)
            }
            postDao.insertCommentReaction(com.example.data.model.CommentReactionEntity(commentId, "DOWNVOTE"))
            newDownCount++
        }

        postDao.updateComment(
            comment.copy(
                upvotesCount = newUpCount,
                downvotesCount = newDownCount
            )
        )
        
        enqueueOperation("TOGGLE_COMMENT_REACTION", JSONObject().apply {
            put("commentId", commentId)
            put("reactionType", "DOWNVOTE")
        })
    }
}
