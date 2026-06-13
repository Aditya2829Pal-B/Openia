package com.example.data.repository

import com.example.data.local.PostDao
import com.example.data.model.CommentEntity
import com.example.data.model.PostEntity
import com.example.data.model.UserReactionEntity
import kotlinx.coroutines.flow.Flow

import com.example.data.model.UserProfileEntity
import com.example.data.model.FollowEntity

class PostRepository(private val postDao: PostDao) {

    val allPosts: Flow<List<PostEntity>> = postDao.getAllPostsFlow()

    val myProfile: Flow<UserProfileEntity?> = postDao.getUserProfileFlow("You")

    val allFollows: Flow<List<FollowEntity>> = postDao.getFollowsFlow()

    suspend fun getProfileDirect(username: String): UserProfileEntity? = postDao.getUserProfile(username)

    suspend fun updateProfile(profile: UserProfileEntity) {
        postDao.insertUserProfile(profile)
    }

    suspend fun toggleFollow(authorName: String) {
        val existing = postDao.getFollowByAuthor(authorName)
        if (existing != null) {
            postDao.deleteFollow(existing)
        } else {
            postDao.insertFollow(FollowEntity(authorName))
        }
    }

    fun getPostById(id: Int): Flow<PostEntity?> = postDao.getPostByIdFlow(id)

    suspend fun getPostByIdDirect(id: Int): PostEntity? = postDao.getPostById(id)

    fun getCommentsForPost(postId: Int): Flow<List<CommentEntity>> = 
        postDao.getCommentsByPostFlow(postId)

    fun getUserReactionsForPost(postId: Int): Flow<List<UserReactionEntity>> =
        postDao.getUserReactionsForPostFlow(postId)

    suspend fun createPost(post: PostEntity): Long = postDao.insertPost(post)

    suspend fun deletePost(post: PostEntity) = postDao.deletePost(post)

    suspend fun updatePost(post: PostEntity) = postDao.updatePost(post)

    suspend fun createComment(comment: CommentEntity) {
        postDao.insertComment(comment)
        // Also increment comment count on post
        val post = postDao.getPostById(comment.postId)
        if (post != null) {
            postDao.updatePost(post.copy(commentCount = post.commentCount + 1))
        }
    }

    suspend fun deleteComment(comment: CommentEntity) {
        postDao.deleteComment(comment)
        val post = postDao.getPostById(comment.postId)
        if (post != null) {
            postDao.updatePost(post.copy(commentCount = maxOf(0, post.commentCount - 1)))
        }
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
    }
}
