package com.example.data.realtime

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "pending_operations")
data class PendingOperation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val operationType: String, // e.g. "CREATE_POST", "SYNC_REACTION"
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface PendingOperationDao {
    @Query("SELECT * FROM pending_operations ORDER BY timestamp ASC")
    fun getAllPendingOperations(): Flow<List<PendingOperation>>

    @Query("SELECT * FROM pending_operations ORDER BY timestamp ASC")
    suspend fun getAllPendingOperationsSync(): List<PendingOperation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: PendingOperation)

    @Delete
    suspend fun deleteOperation(operation: PendingOperation)
}
