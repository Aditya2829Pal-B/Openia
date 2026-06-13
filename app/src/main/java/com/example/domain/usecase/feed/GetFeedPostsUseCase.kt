package com.example.domain.usecase.feed

import com.example.data.model.PostEntity
import com.example.data.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GetFeedPostsUseCase(private val repository: PostRepository) {

    fun execute(
        selectedType: Flow<String>,
        selectedCategory: Flow<String>,
        searchQuery: Flow<String>,
        selectedSort: Flow<String>,
        followedOnly: Flow<Boolean>,
        allFollows: Flow<List<String>>
    ): Flow<List<PostEntity>> {
        return combine(
            repository.allPosts,
            selectedType,
            selectedCategory,
            searchQuery,
            selectedSort,
            followedOnly,
            allFollows
        ) { args ->
            @Suppress("UNCHECKED_CAST")
            val allPosts = args[0] as List<PostEntity>
            val type = args[1] as String
            val category = args[2] as String
            val query = args[3] as String
            val sort = args[4] as String
            val followedOnlyFlag = args[5] as Boolean
            @Suppress("UNCHECKED_CAST")
            val followsList = args[6] as List<String>

            var list = allPosts

            // Filter Followed only
            if (followedOnlyFlag) {
                list = list.filter { followsList.contains(it.author) }
            }

            // Filter Type
            if (type != "ALL") {
                list = list.filter { it.postType == type }
            }

            // Filter Category
            if (category != "ALL") {
                list = list.filter { it.category.equals(category, ignoreCase = true) }
            }

            // Search Query
            if (query.isNotEmpty()) {
                list = list.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true) ||
                    it.tags.contains(query, ignoreCase = true)
                }
            }

            // Sort
            val sortedList = if (sort == "LATEST") {
                list.sortedByDescending { it.timestamp }
            } else {
                // Intelligent Quality-First Timeline
                // HackerNews style gravity decay but weighted for positive engagement depth
                val now = System.currentTimeMillis()
                list.sortedByDescending { post ->
                    val ageHours = (now - post.timestamp) / (1000f * 60f * 60f)
                    val baseScore = (post.agreeCount * 3f) + (post.upvotesCount * 2f) + (post.empathyCount * 4f) + (post.commentCount * 5f) - (post.disagreeCount * 2f) - (post.downvotesCount * 1f)
                    val decay = Math.pow((ageHours + 2f).toDouble(), 1.8).toFloat()
                    maxOf(0f, baseScore / decay)
                }
            }
            sortedList
        }
    }
}
