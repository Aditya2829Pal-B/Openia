package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscussionsDao {
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPostsFlow(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getPagingSource(): androidx.paging.PagingSource<Int, PostEntity>

    @Query("""
        SELECT * FROM posts 
        WHERE (:type = 'ALL' OR postType = :type)
        AND (:category = 'ALL' OR category = :category)
        AND (:query = '' OR title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
        AND (:followedOnly = 0 OR author IN (:follows) OR (author = 'dummy_no_follows_so_fail' AND 1=0))
        ORDER BY timestamp DESC
    """)
    fun getDynamicPagingSource(
        type: String, 
        category: String, 
        query: String, 
        followedOnly: Boolean, 
        follows: List<String>
    ): androidx.paging.PagingSource<Int, PostEntity>

    @Query("SELECT * FROM posts WHERE id = :id")
    fun getPostByIdFlow(id: Int): Flow<PostEntity?>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getPostById(id: Int): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity): Long

    @Update
    suspend fun updatePost(post: PostEntity)

    @Delete
    suspend fun deletePost(post: PostEntity)

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsByPostFlow(postId: Int): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity): Long

    @Delete
    suspend fun deleteComment(comment: CommentEntity)

    @Query("SELECT * FROM user_reactions WHERE postId = :postId")
    fun getUserReactionsForPostFlow(postId: Int): Flow<List<UserReactionEntity>>

    @Query("SELECT * FROM user_reactions WHERE postId = :postId")
    suspend fun getUserReactionsForPost(postId: Int): List<UserReactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReaction(reaction: UserReactionEntity)

    @Delete
    suspend fun deleteReaction(reaction: UserReactionEntity)

    @Query("SELECT * FROM user_reactions WHERE postId = :postId AND reactionType = :reactionType")
    suspend fun getSpecificReaction(postId: Int, reactionType: String): UserReactionEntity?

    @Query("SELECT * FROM comment_reactions WHERE commentId = :commentId")
    fun getReactionsForCommentFlow(commentId: Int): Flow<List<CommentReactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommentReaction(reaction: CommentReactionEntity)

    @Delete
    suspend fun deleteCommentReaction(reaction: CommentReactionEntity)

    @Query("SELECT * FROM comment_reactions WHERE commentId = :commentId AND reactionType = :reactionType")
    suspend fun getSpecificCommentReaction(commentId: Int, reactionType: String): CommentReactionEntity?

    @Query("SELECT * FROM comments WHERE id = :id")
    suspend fun getCommentById(id: Int): CommentEntity?

    @Update
    suspend fun updateComment(comment: CommentEntity)

    @Query("SELECT * FROM user_profile WHERE username = :username")
    fun getUserProfileFlow(username: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE username = :username")
    suspend fun getUserProfile(username: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM follows")
    fun getFollowsFlow(): Flow<List<FollowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity)

    @Delete
    suspend fun deleteFollow(follow: FollowEntity)

    @Query("SELECT * FROM follows WHERE followedAuthor = :authorName")
    suspend fun getFollowByAuthor(authorName: String): FollowEntity?

    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarksFlow(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("SELECT * FROM bookmarks WHERE postId = :postId")
    suspend fun getBookmarkByPostId(postId: Int): BookmarkEntity?

    @Query("SELECT * FROM drafts ORDER BY timestamp DESC")
    fun getAllDraftsFlow(): Flow<List<DraftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftEntity)

    @Query("DELETE FROM drafts WHERE id = :id")
    suspend fun deleteDraftById(id: Int)
}
