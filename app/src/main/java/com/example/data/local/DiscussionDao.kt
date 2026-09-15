package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Discussion
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscussionDao {
    @Query("SELECT * FROM discussions ORDER BY createdAt DESC")
    fun getAllDiscussions(): Flow<List<Discussion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscussion(discussion: Discussion): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscussions(discussions: List<Discussion>)
}
