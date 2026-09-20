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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PostEntity
import com.example.domain.model.AdvancedReputation
import com.example.ui.theme.*
import com.example.ui.viewmodel.PostViewModel

@Composable
fun CommunityTabContent(
    viewModel: PostViewModel,
    headerContent: @Composable () -> Unit,
    posts: List<PostEntity>,
    reputations: Map<String, Int>,
    advancedReputations: Map<String, AdvancedReputation>
) {
    val discussions by viewModel.allDiscussions.collectAsStateWithLifecycle()
    val trendingPosts = remember(posts) {
        posts.sortedByDescending { (it.upvotesCount - it.downvotesCount) + it.commentCount }.take(10)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBlack),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                headerContent()
                
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = "Community Highlights",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Actionable problem solving & trending discussions",
                        color = SoftText,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }

        // Actionable discussions section
        if (discussions.isNotEmpty()) {
            item {
                Text(
                    text = "Actionable Problem Discussions",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            items(discussions, key = { "disc_${it.id}" }) { discussion ->
                DiscussionCard(
                    discussion = discussion,
                    onClick = { viewModel.selectDiscussion(discussion.id) },
                    onUpvote = { viewModel.upvoteDiscussion(discussion.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Trending Thoughts & Debates",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
        }

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
