package com.example.domain.usecase.followers

import com.example.data.repository.PostRepository

class ToggleFollowUseCase(private val repository: PostRepository) {
    suspend fun execute(authorName: String) {
        repository.toggleFollow(authorName)
    }
}
