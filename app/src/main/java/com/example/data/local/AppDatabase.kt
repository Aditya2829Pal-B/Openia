package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CommentEntity
import com.example.data.model.PostEntity
import com.example.data.model.UserReactionEntity
import com.example.data.model.CommentReactionEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.FollowEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.DraftEntity

import com.example.data.realtime.PendingOperation
import com.example.data.realtime.PendingOperationDao

@Database(
    entities = [
        PostEntity::class,
        CommentEntity::class,
        UserReactionEntity::class,
        CommentReactionEntity::class,
        UserProfileEntity::class,
        FollowEntity::class,
        PendingOperation::class,
        BookmarkEntity::class,
        DraftEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun discussionsDao(): DiscussionsDao
    abstract fun pendingOperationDao(): PendingOperationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "openia_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
