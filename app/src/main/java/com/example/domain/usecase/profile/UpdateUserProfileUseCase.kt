package com.example.domain.usecase.profile

import com.example.data.model.UserProfileEntity
import com.example.data.repository.PostRepository

class UpdateUserProfileUseCase(private val repository: PostRepository) {
    suspend fun execute(displayName: String, bio: String, avatarSeed: String) {
        val updated = UserProfileEntity(
            username = "You",
            displayName = displayName,
            bio = bio,
            avatarSeed = avatarSeed
        )
        repository.updateProfile(updated)
    }
}
