package com.example.data.repository

import com.example.data.local.DiscussionDao
import com.example.data.model.Discussion
import kotlinx.coroutines.flow.Flow

class DiscussionRepository(private val discussionDao: DiscussionDao) : BaseRepository() {

    fun getAllDiscussions(): Flow<List<Discussion>> {
        return discussionDao.getAllDiscussions()
    }

    suspend fun insertDiscussion(discussion: Discussion) {
        safeDatabaseCall { discussionDao.insertDiscussion(discussion) }
    }
}
