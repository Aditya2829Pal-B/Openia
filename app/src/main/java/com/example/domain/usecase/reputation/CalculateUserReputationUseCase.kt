package com.example.domain.usecase.reputation

import com.example.data.model.PostEntity
import com.example.data.repository.PostRepository
import com.example.domain.model.AdvancedReputation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CalculateUserReputationUseCase(private val repository: PostRepository) {

    fun executeDetailed(): Flow<Map<String, AdvancedReputation>> {
        return repository.allPosts.map { allPosts ->
            val map = mutableMapOf<String, AdvancedReputation>()
            
            // Collect base stats
            val authorPoints = mutableMapOf<String, Int>()
            authorPoints["NavalS"] = 85
            authorPoints["AnxiousCoder"] = 10
            authorPoints["EcoWarrior"] = 110
            authorPoints["PrivacySceptic"] = 40
            authorPoints["You"] = 0

            val authorAgrees = mutableMapOf<String, Int>()
            val authorEmpathies = mutableMapOf<String, Int>()
            val authorUpvotes = mutableMapOf<String, Int>()
            val authorPosts = mutableMapOf<String, Int>()

            for (post in allPosts) {
                val author = post.author
                
                val currentPoints = authorPoints.getOrDefault(author, 0)
                val postPoints = (post.agreeCount * 5) - (post.disagreeCount * 1) +
                                 (post.upvotesCount * 2) - (post.downvotesCount * 1) +
                                 (post.empathyCount * 5)
                                 
                authorPoints[author] = currentPoints + postPoints
                authorAgrees[author] = authorAgrees.getOrDefault(author, 0) + post.agreeCount
                authorEmpathies[author] = authorEmpathies.getOrDefault(author, 0) + post.empathyCount
                authorUpvotes[author] = authorUpvotes.getOrDefault(author, 0) + post.upvotesCount
                authorPosts[author] = authorPosts.getOrDefault(author, 0) + 1
            }

            for ((author, points) in authorPoints) {
                val postsCount = authorPosts.getOrDefault(author, 1).coerceAtLeast(1)
                val empathies = authorEmpathies.getOrDefault(author, 0).toFloat()
                val agrees = authorAgrees.getOrDefault(author, 0).toFloat()
                val upvotes = authorUpvotes.getOrDefault(author, 0).toFloat()
                
                val empathyScore = (empathies / postsCount / 10f).coerceIn(0f, 1f)
                val consensusScore = (agrees / postsCount / 10f).coerceIn(0f, 1f)
                val insightScore = (upvotes / postsCount / 5f).coerceIn(0f, 1f)
                val trustScore = (0.5f + (consensusScore * 0.2f) + (insightScore * 0.3f)).coerceIn(0f, 1f)
                
                val prestigeLevel = points / 200
                val rankLevel = (points % 200) / 40 + 1
                
                val rankName = when {
                    points < 10 -> "Observer"
                    points in 10..30 -> "Seeker"
                    points in 31..70 -> "Analyst"
                    points in 71..150 -> "Luminary"
                    else -> "Architect"
                }

                val civilizationClass = when {
                    prestigeLevel == 0 -> "Type 0.1 (Initiate)"
                    prestigeLevel == 1 -> "Type 0.2 (Explorer)"
                    prestigeLevel in 2..5 -> "Type 0.4 (Cultivator)"
                    else -> "Type 0.8 (Visionary)"
                }

                val archetype = when {
                    empathyScore > 0.7f && consensusScore > 0.6f -> "Harmonizer"
                    insightScore > 0.8f -> "Deep Synthesizer"
                    trustScore > 0.8f -> "Truth Anchor"
                    else -> "Generalist Thinker"
                }

                val domain = when {
                    postsCount > 10 -> "Global Strategy"
                    agrees > 50 -> "Social Dynamics"
                    else -> "Emergent Ideas"
                }
                
                map[author] = AdvancedReputation(
                    totalScore = points,
                    trustScore = trustScore,
                    empathyScore = empathyScore,
                    insightScore = insightScore,
                    consensusScore = consensusScore,
                    rankName = rankName,
                    rankLevel = rankLevel,
                    prestigeLevel = prestigeLevel,
                    thinkerArchetype = archetype,
                    influenceDomain = domain,
                    civilizationClass = civilizationClass
                )
            }
            
            map
        }
    }
    
    fun execute(): Flow<Map<String, Int>> {
        return executeDetailed().map { detailedMap ->
            detailedMap.mapValues { it.value.totalScore }
        }
    }
}
