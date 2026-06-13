package com.example.domain.usecase.feed

import com.example.data.model.PostEntity
import com.example.data.repository.PostRepository

class CreatePostUseCase(private val repository: PostRepository) {
    suspend fun execute(
        title: String,
        content: String,
        type: String, // "OPINION" or "PROBLEM"
        category: String,
        tags: String,
        imageUri: String? = null,
        author: String = "You"
    ): Long {
        val validCategory = if (category == "ALL") "General" else category
        val avatarSeed = ('A'..'Z').random().toString()
        val post = PostEntity(
            postType = type,
            title = title,
            content = content,
            category = validCategory,
            tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.joinToString(","),
            author = author,
            avatarSeed = avatarSeed,
            imageUri = imageUri,
            timestamp = System.currentTimeMillis()
        )
        return repository.createPost(post)
    }
}
