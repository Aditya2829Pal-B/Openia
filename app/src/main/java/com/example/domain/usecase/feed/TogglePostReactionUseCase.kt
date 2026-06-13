package com.example.domain.usecase.feed

import com.example.data.repository.PostRepository

class TogglePostReactionUseCase(private val repository: PostRepository) {
    suspend fun execute(postId: Int, reactionType: String) {
        when (reactionType.uppercase()) {
            "AGREE" -> repository.toggleAgree(postId)
            "DISAGREE" -> repository.toggleDisagree(postId)
            "UPVOTE" -> repository.toggleUpvote(postId)
            "DOWNVOTE" -> repository.toggleDownvote(postId)
            "EMPATHY" -> repository.toggleEmpathy(postId)
        }
    }
}
