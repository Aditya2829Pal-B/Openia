package com.example.domain.usecase.analytics

import com.example.data.model.PostEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TrendingTag(
    val tag: String,
    val count: Int
)

class GetTrendMetricsUseCase {

    fun extractTrendingTags(posts: List<PostEntity>): List<TrendingTag> {
        val tagCounts = mutableMapOf<String, Int>()
        posts.forEach { post ->
            post.tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { tag ->
                    tagCounts[tag] = tagCounts.getOrDefault(tag, 0) + 1
                }
        }
        return tagCounts.entries
            .map { TrendingTag(it.key, it.value) }
            .sortedByDescending { it.count }
            .take(6)
    }

    fun getGeneralCategoryMetrics(posts: List<PostEntity>): Map<String, Float> {
        if (posts.isEmpty()) return emptyMap()
        val totals = posts.size.toFloat()
        return posts.groupBy { it.category }
            .mapValues { (_, value) -> value.size.toFloat() / totals }
    }
}
