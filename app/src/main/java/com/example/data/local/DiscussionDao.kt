package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Discussion
import com.example.data.model.ReputationPoint
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscussionDao {
    @Query("SELECT * FROM discussions ORDER BY createdAt DESC")
    fun getAllDiscussions(): Flow<List<Discussion>>

    @Query("SELECT * FROM discussions WHERE id = :id")
    fun getDiscussionByIdFlow(id: Int): Flow<Discussion?>

    @Query("SELECT * FROM discussions WHERE id = :id")
    suspend fun getDiscussionById(id: Int): Discussion?

    @Query("UPDATE discussions SET upvotes = upvotes + 1 WHERE id = :id")
    suspend fun upvoteDiscussion(id: Int)

    @Query("UPDATE discussions SET downvotes = downvotes + 1 WHERE id = :id")
    suspend fun downvoteDiscussion(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscussion(discussion: Discussion): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscussions(discussions: List<Discussion>)

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserByIdFlow(id: Int): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Int): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReputationPoint(point: ReputationPoint): Long

    @Query("SELECT * FROM reputation_points WHERE userId = :userId ORDER BY awardedAt DESC")
    fun getReputationPointsForUserFlow(userId: Int): Flow<List<ReputationPoint>>
}

