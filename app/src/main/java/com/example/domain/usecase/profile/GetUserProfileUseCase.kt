package com.example.domain.usecase.profile

import com.example.data.model.UserProfileEntity
import com.example.data.repository.PostRepository
import kotlinx.coroutines.flow.Flow

class GetUserProfileUseCase(private val repository: PostRepository) {
    fun execute(username: String): Flow<UserProfileEntity?> {
        return repository.myProfile
    }

    suspend fun executeDirect(username: String): UserProfileEntity? {
        if (username == "You") {
            return repository.getLocalProfileDirect()
        }
        return repository.getProfileDirect(username)
    }
}
