package com.example.domain.usecase.moderation

class ModerateContentUseCase {

    private val spamKeywords = setOf("spam", "scam", "malware", "phishing", "click here", "buy now", "$$", "free money")
    private val toxicKeywords = setOf("hate", "moron", "idiot", "die ", "kill ", "stupid")

    fun isContentSafe(title: String, content: String): Boolean {
        val lowercaseTitle = title.lowercase()
        val lowercaseContent = content.lowercase()
        
        val containsSpam = spamKeywords.any { lowercaseTitle.contains(it) || lowercaseContent.contains(it) }
        val containsToxicity = toxicKeywords.any { lowercaseTitle.contains(it) || lowercaseContent.contains(it) }
        
        // Basic bot-behavior analysis: content with high repetition or extreme length without spaces
        val isRepetitive = lowercaseContent.split(" ").groupingBy { it }.eachCount().any { it.value > 10 && it.key.length > 3 }
        val isMaliciousLayout = lowercaseContent.split(" ").any { it.length > 50 } // Word without spaces

        return !containsSpam && !containsToxicity && !isRepetitive && !isMaliciousLayout
    }

    fun reportPost(postId: Int, reason: String): Result<Unit> {
        // Integrate with trust-weighted reporting formula in backend
        return Result.success(Unit)
    }
}
