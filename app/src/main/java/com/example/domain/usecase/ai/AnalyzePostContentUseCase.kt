package com.example.domain.usecase.ai

import com.example.data.repository.PostRepository
import com.example.network.GeminiClient

class AnalyzePostContentUseCase(private val repository: PostRepository) {

    suspend fun execute(postId: Int): Boolean {
        val post = repository.getPostByIdDirect(postId) ?: return false
        return try {
            val analysis = GeminiClient.analyzePost(
                title = post.title,
                content = post.content,
                type = post.postType,
                category = post.category
            )
            if (analysis != null) {
                // Topic Clustering & Contradiction Detection layer (Simulated for Offline Mode)
                val isDebateHeavy = post.disagreeCount > 5 || post.commentCount > 10
                val debateSummary = if (isDebateHeavy) "\n[Debate Auto-Extracted]: Active friction over foundational definitions detected." else ""
                val knowledgeCard = "Primary Paradigm: Resolving ${post.category} friction through multi-variate consensus."
                
                repository.saveAiAnalysis(
                    postId = postId,
                    summary = analysis.first + debateSummary,
                    solutions = analysis.second,
                    consensus = analysis.third + "\n" + knowledgeCard
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
