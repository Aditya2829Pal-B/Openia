package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Discussion
import com.example.ui.theme.*
import com.example.ui.viewmodel.PostViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiscussionFeedScreen(
    viewModel: PostViewModel,
    modifier: Modifier = Modifier,
    onDiscussionClick: ((Discussion) -> Unit)? = null
) {
    val discussions by viewModel.allDiscussions.collectAsStateWithLifecycle()
    val selectedDiscussionId by viewModel.selectedDiscussionId.collectAsStateWithLifecycle()
    val currentDiscussion by viewModel.currentDiscussion.collectAsStateWithLifecycle()

    // If a discussion is currently selected, show the detail screen directly
    if (selectedDiscussionId != null && currentDiscussion != null) {
        DiscussionDetailScreen(
            discussion = currentDiscussion!!,
            viewModel = viewModel,
            onBack = { viewModel.selectDiscussion(null) }
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBlack),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                Text(
                    text = "Discussion & Problem Hub",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Collaborate on complex challenges, explore nested discussion trees, and contribute actionable solutions.",
                    color = SoftText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        if (discussions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CosmicDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No discussions yet",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Discussions and actionable challenges will appear here.",
                            color = SoftText,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(discussions, key = { it.id }) { discussion ->
                DiscussionCard(
                    discussion = discussion,
                    onClick = {
                        if (onDiscussionClick != null) {
                            onDiscussionClick(discussion)
                        } else {
                            viewModel.selectDiscussion(discussion.id)
                        }
                    },
                    onUpvote = {
                        viewModel.upvoteDiscussion(discussion.id)
                    }
                )
            }
        }
    }
}

@Composable
fun DiscussionCard(
    discussion: Discussion,
    onClick: () -> Unit = {},
    onUpvote: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discussion_card_${discussion.id}")
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CosmicDark),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CosmicGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Category Badge & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeoCyan.copy(alpha = 0.12f))
                        .border(1.dp, NeoCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = discussion.category,
                        color = NeoCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(discussion.createdAt)),
                    color = SoftText,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Discussion Title
            Text(
                text = discussion.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Content snippet
            Text(
                text = discussion.content,
                style = MaterialTheme.typography.bodyMedium,
                color = HeaderText.copy(alpha = 0.85f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Actionable problem badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SolarCoral.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = SolarCoral,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Actionable Problem",
                                color = SolarCoral,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Upvote count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onUpvote() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Upvotes",
                            tint = NeoCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${discussion.upvotes - discussion.downvotes}",
                            color = NeoCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // CTA to open detail
                Text(
                    text = "View & Solve →",
                    color = NeoCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
