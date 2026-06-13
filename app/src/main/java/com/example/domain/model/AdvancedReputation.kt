package com.example.domain.model

data class AdvancedReputation(
    val totalScore: Int = 0,
    val trustScore: Float = 0.5f,
    val empathyScore: Float = 0.5f,
    val insightScore: Float = 0.5f,
    val consensusScore: Float = 0.5f,
    val rankName: String = "Observer",
    val rankLevel: Int = 1,
    val prestigeLevel: Int = 0,
    val thinkerArchetype: String = "Synthesizer",
    val influenceDomain: String = "Philosophy & Logic",
    val civilizationClass: String = "Type 0.1"
) {
    val isSpammer: Boolean
        get() = trustScore < 0.2f
}
