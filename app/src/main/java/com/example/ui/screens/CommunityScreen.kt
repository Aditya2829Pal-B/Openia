package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostEntity
import com.example.domain.model.AdvancedReputation
import com.example.ui.theme.*
import com.example.ui.viewmodel.PostViewModel

@Composable
fun CommunityTabContent(
    viewModel: PostViewModel,
    posts: List<PostEntity>,
    reputations: Map<String, Int>,
    advancedReputations: Map<String, AdvancedReputation>
) {
    val trendingPosts = remember(posts) {
        posts.sortedByDescending { (it.upvotesCount - it.downvotesCount) + it.commentCount }.take(10)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBlack)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Community Highlights",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Top discussions happening right now",
                color = SoftText,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(trendingPosts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    followedAuthors = emptyList(), // or pass if needed
                    reputations = reputations,
                    advancedReputations = advancedReputations,
                    onFollowToggle = { viewModel.toggleFollow(it) },
                    onCardClick = { viewModel.selectPost(post.id) },
                    onAgreeClick = { viewModel.agreePost(post.id) },
                    onDisagreeClick = { viewModel.disagreePost(post.id) },
                    onUpvoteClick = { viewModel.upvotePost(post.id) },
                    onDownvoteClick = { viewModel.downvotePost(post.id) },
                    onEmpathyClick = { viewModel.empathyPost(post.id) }
                )
            }
        }
    }
}
