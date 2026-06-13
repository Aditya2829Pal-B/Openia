package com.example.domain.usecase.feed

import com.example.data.model.CommentEntity
import com.example.data.repository.PostRepository

class AddPostCommentUseCase(private val repository: PostRepository) {
    suspend fun execute(postId: Int, content: String, author: String = "You", isSolution: Boolean = false) {
        val avatarSeed = ('A'..'Z').random().toString()
        val comment = CommentEntity(
            postId = postId,
            author = author,
            avatarSeed = avatarSeed,
            content = content,
            timestamp = System.currentTimeMillis(),
            isSolution = isSolution
        )
        repository.createComment(comment)
    }
}
