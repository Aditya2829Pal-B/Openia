package com.example.domain.usecase.profile

import com.example.data.model.UserProfileEntity
import com.example.data.repository.PostRepository
import kotlinx.coroutines.flow.Flow

class GetUserProfileUseCase(private val repository: PostRepository) {
    fun execute(username: String): Flow<UserProfileEntity?> {
        return repository.myProfile // Under existing DB structure, username is currently "You"
    }

    suspend fun executeDirect(username: String): UserProfileEntity? {
        return repository.getProfileDirect(username)
    }
}
