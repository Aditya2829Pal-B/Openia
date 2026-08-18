package com.example.domain.usecase.profile

import com.example.data.model.UserProfileEntity
import com.example.data.repository.PostRepository

class UpdateUserProfileUseCase(private val repository: PostRepository) {
    suspend fun execute(oldUsername: String, newUsername: String, displayName: String, bio: String, avatarSeed: String, profilePictureUri: String?) {
        val updated = UserProfileEntity(
            username = newUsername,
            displayName = displayName,
            bio = bio,
            avatarSeed = avatarSeed,
            profilePictureUri = profilePictureUri
        )
        // If username changed, we should ideally insert new and delete old, but for simplicity, we just insert.
        // Wait, SQLite will keep the old one if we change the PK.
        if (oldUsername != newUsername) {
            repository.getProfileDirect(oldUsername)?.let {
                repository.deleteProfile(oldUsername)
            }
        }
        repository.updateProfile(updated)
    }
}
