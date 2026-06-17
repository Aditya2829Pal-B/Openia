package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.ui.screens.PostCard
import com.example.ui.screens.NeoCyan
import com.example.ui.viewmodel.PostViewModel

@Composable
fun DiscussionFeed(
    viewModel: PostViewModel,
    modifier: Modifier = Modifier
) {
    val pagedPosts = viewModel.pagedPosts.collectAsLazyPagingItems()
    val allFollows by viewModel.allFollows.collectAsStateWithLifecycle()
    val authorReputations by viewModel.authorReputations.collectAsStateWithLifecycle()
    val advancedReputations by viewModel.advancedReputations.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarkedPostIds.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            count = pagedPosts.itemCount,
            key = { index -> pagedPosts[index]?.id ?: "placeholder_$index" }
        ) { index ->
            val post = pagedPosts[index]
            if (post != null) {
                PostCard(
                    post = post,
                    followedAuthors = allFollows,
                    reputations = authorReputations,
                    advancedReputations = advancedReputations,
                    isBookmarked = bookmarks.contains(post.id),
                    onFollowToggle = { viewModel.toggleFollow(it) },
                    onBookmarkToggle = { viewModel.toggleBookmark(post.id) },
                    onCardClick = { viewModel.selectPost(post.id) },
                    onAgreeClick = { viewModel.agreePost(post.id) },
                    onDisagreeClick = { viewModel.disagreePost(post.id) },
                    onUpvoteClick = { viewModel.upvotePost(post.id) },
                    onDownvoteClick = { viewModel.downvotePost(post.id) },
                    onEmpathyClick = { viewModel.empathyPost(post.id) }
                )
            }
        }

        if (pagedPosts.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeoCyan)
                }
            }
        } else if (pagedPosts.loadState.append is LoadState.Error) {
            item {
                Text(
                    text = "Error loading more posts. Tap to retry.",
                    color = Color.Red,
                    modifier = Modifier.clickable { pagedPosts.retry() }
                )
            }
        }
    }
}
