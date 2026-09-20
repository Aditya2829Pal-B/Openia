package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CommentEntity
import com.example.data.model.Discussion
import com.example.ui.theme.*
import com.example.ui.viewmodel.PostViewModel

/**
 * Detail screen for discussions that displays:
 * 1. The full discussion content, author info, category, tags, and stats.
 * 2. Nested comments using a recursive structure.
 * 3. An 'Actionable' button to contribute to problem-solving.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiscussionDetailScreen(
    discussion: Discussion,
    viewModel: PostViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep local discussion synchronized if live state updates in Room
    val liveDiscussion by viewModel.currentDiscussion.collectAsStateWithLifecycle()
    val activeDiscussion = liveDiscussion?.takeIf { it.id == discussion.id } ?: discussion

    val comments by viewModel.currentDiscussionComments.collectAsStateWithLifecycle()
    val myProfile by viewModel.myProfile.collectAsStateWithLifecycle()

    var commentText by remember { mutableStateOf("") }
    var isActionableSolution by remember { mutableStateOf(false) }
    var replyingToComment by remember { mutableStateOf<CommentEntity?>(null) }
    var showActionableDialog by remember { mutableStateOf(false) }
    var userVotedUp by remember { mutableStateOf(false) }
    var userVotedDown by remember { mutableStateOf(false) }

    // Intercept hardware/system back gesture
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBlack),
        containerColor = CosmicBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Discussion Details",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("discussion_detail_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Category pill
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeoCyan.copy(alpha = 0.15f))
                            .border(1.dp, NeoCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = activeDiscussion.category,
                            color = NeoCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CosmicDark,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            // Persistent bottom reply input bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding(),
                color = CosmicDark,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Replying to banner
                    if (replyingToComment != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = NeoCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Replying to @${replyingToComment?.author ?: "thinker"}",
                                    color = NeoCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(
                                onClick = { replyingToComment = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    tint = SoftText,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Mark as actionable toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isActionableSolution,
                            onCheckedChange = { isActionableSolution = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = SolarCoral,
                                uncheckedColor = SoftText
                            ),
                            modifier = Modifier.testTag("toggle_actionable_solution")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Contribute as Actionable Solution",
                            color = if (isActionableSolution) SolarCoral else SoftText,
                            fontSize = 12.sp,
                            fontWeight = if (isActionableSolution) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = {
                                Text(
                                    text = if (isActionableSolution) {
                                        "Describe your actionable solution..."
                                    } else if (replyingToComment != null) {
                                        "Write a nested reply..."
                                    } else {
                                        "Contribute to this discussion..."
                                    },
                                    color = SoftText,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("discussion_comment_input"),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isActionableSolution) SolarCoral else NeoCyan,
                                unfocusedBorderColor = CosmicGray,
                                focusedContainerColor = CosmicInput,
                                unfocusedContainerColor = CosmicInput,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (commentText.trim().isNotEmpty()) {
                                    viewModel.submitDiscussionComment(
                                        discussionId = activeDiscussion.id,
                                        content = commentText.trim(),
                                        author = myProfile?.displayName ?: myProfile?.username ?: "You",
                                        isSolution = isActionableSolution,
                                        parentCommentId = replyingToComment?.id
                                    )
                                    commentText = ""
                                    isActionableSolution = false
                                    replyingToComment = null
                                }
                            },
                            modifier = Modifier
                                .background(
                                    if (isActionableSolution) SolarCoral else NeoCyan,
                                    CircleShape
                                )
                                .size(44.dp)
                                .testTag("send_discussion_comment_btn"),
                            enabled = commentText.trim().isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send Comment",
                                tint = CosmicBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. FULL DISCUSSION CONTENT CARD
            item {
                DiscussionFullContentCard(
                    discussion = activeDiscussion,
                    userVotedUp = userVotedUp,
                    userVotedDown = userVotedDown,
                    onUpvote = {
                        if (!userVotedUp) {
                            viewModel.upvoteDiscussion(activeDiscussion.id)
                            userVotedUp = true
                            userVotedDown = false
                        }
                    },
                    onDownvote = {
                        if (!userVotedDown) {
                            viewModel.downvoteDiscussion(activeDiscussion.id)
                            userVotedDown = true
                            userVotedUp = false
                        }
                    },
                    onActionableClick = { showActionableDialog = true }
                )
            }

            // 2. ACTIONABLE PROBLEM SOLVING HIGHLIGHT BANNER
            item {
                ActionableBanner(
                    solutionCount = comments.count { it.isSolution },
                    onContributeActionable = { showActionableDialog = true }
                )
            }

            // 3. COMMENTS & DISCUSSION TREE HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Discussion Tree (${comments.size} replies)",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Small Actionable quick trigger
                    Button(
                        onClick = { showActionableDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolarCoral,
                            contentColor = CosmicBlack
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("actionable_button_header")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Actionable",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // 4. NESTED RECURSIVE COMMENTS STRUCTURE
            if (comments.isEmpty()) {
                item {
                    EmptyCommentsCard(onActionableClick = { showActionableDialog = true })
                }
            } else {
                item {
                    RecursiveCommentsTree(
                        comments = comments,
                        onReply = { comment ->
                            replyingToComment = comment
                        },
                        onUpvote = { commentId ->
                            viewModel.upvoteComment(commentId)
                        },
                        onDownvote = { commentId ->
                            viewModel.downvoteComment(commentId)
                        }
                    )
                }
            }
        }
    }

    // Modal Dialog to contribute actionable problem-solving resolution
    if (showActionableDialog) {
        ContributeActionableSolutionDialog(
            discussionTitle = activeDiscussion.title,
            onDismiss = { showActionableDialog = false },
            onSubmit = { title, steps, outcome ->
                viewModel.contributeActionableSolution(
                    discussionId = activeDiscussion.id,
                    title = title,
                    steps = steps,
                    outcome = outcome
                )
                showActionableDialog = false
            }
        )
    }
}

/**
 * Card displaying the full content of the discussion, author details, tags, and interactive upvote/downvote bar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscussionFullContentCard(
    discussion: Discussion,
    userVotedUp: Boolean,
    userVotedDown: Boolean,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    onActionableClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CosmicDark),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CosmicGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Author information row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val authorSeed = "U${discussion.authorId}"
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(generateAvatarBrush(authorSeed), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = authorSeed,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Author #${discussion.authorId}",
                            color = HeaderText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Contributor",
                            tint = NeoCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = formatFullDate(discussion.createdAt),
                        color = SoftText,
                        fontSize = 11.sp
                    )
                }

                // Upvote & Downvote Counter
                Row(
                    modifier = Modifier
                        .background(CosmicGray, RoundedCornerShape(20.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onUpvote,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("discussion_upvote_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Upvote",
                            tint = if (userVotedUp) NeoCyan else SoftText
                        )
                    }

                    Text(
                        text = "${discussion.upvotes - discussion.downvotes}",
                        color = if (userVotedUp) NeoCyan else if (userVotedDown) SolarCoral else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onDownvote,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("discussion_downvote_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Downvote",
                            tint = if (userVotedDown) SolarCoral else SoftText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Full Discussion Title
            Text(
                text = discussion.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Full Discussion Content
            Text(
                text = discussion.content,
                style = MaterialTheme.typography.bodyLarge,
                color = HeaderText.copy(alpha = 0.95f),
                lineHeight = 24.sp
            )

            // Tags
            if (discussion.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    discussion.tags.split(",").forEach { tag ->
                        val cleanTag = tag.trim().removePrefix("#")
                        if (cleanTag.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CosmicGray)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "#$cleanTag",
                                    color = NeoCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent Actionable problem-solving trigger button
            Button(
                onClick = onActionableClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SolarCoral,
                    contentColor = CosmicBlack
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("actionable_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Actionable — Contribute Problem Solving",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Banner explaining problem-solving collaboration and showing count of actionable solutions.
 */
@Composable
private fun ActionableBanner(
    solutionCount: Int,
    onContributeActionable: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CosmicDark),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(SolarCoral, NeoCyan)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = SolarCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Problem-Solving Hub",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (solutionCount > 0) {
                        "$solutionCount actionable solution${if (solutionCount > 1) "s" else ""} verified by community."
                    } else {
                        "No verified solution yet. Be the first to provide actionable resolution steps."
                    },
                    color = SoftText,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onContributeActionable,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SolarCoral.copy(alpha = 0.2f),
                    contentColor = SolarCoral
                ),
                border = BorderStroke(1.dp, SolarCoral),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("actionable_contribute_banner_btn")
            ) {
                Text(
                    text = "Solve Problem",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Top-level container for the recursive comment tree.
 * Renders all root comments (parentCommentId == null) and recurses down child comments.
 */
@Composable
private fun RecursiveCommentsTree(
    comments: List<CommentEntity>,
    onReply: (CommentEntity) -> Unit,
    onUpvote: (Int) -> Unit,
    onDownvote: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Top-level comments have parentCommentId == null
    val rootComments = remember(comments) {
        comments.filter { it.parentCommentId == null }.sortedByDescending { it.timestamp }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rootComments.forEach { rootComment ->
            RecursiveCommentNode(
                comment = rootComment,
                allComments = comments,
                depth = 0,
                onReply = onReply,
                onUpvote = onUpvote,
                onDownvote = onDownvote
            )
        }
    }
}

/**
 * RECURSIVE COMPOSABLE:
 * Renders an individual comment with depth indentation and thread connecting line,
 * then recursively calls itself for each of its child comments.
 */
@Composable
private fun RecursiveCommentNode(
    comment: CommentEntity,
    allComments: List<CommentEntity>,
    depth: Int = 0,
    maxDepth: Int = 5,
    onReply: (CommentEntity) -> Unit,
    onUpvote: (Int) -> Unit,
    onDownvote: (Int) -> Unit
) {
    // Find nested children for this comment
    val childComments = remember(allComments, comment.id) {
        allComments.filter { it.parentCommentId == comment.id }.sortedBy { it.timestamp }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth.coerceAtMost(maxDepth) * 16).dp)
    ) {
        // Individual Comment Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("comment_item_${comment.id}"),
            colors = CardDefaults.cardColors(
                containerColor = if (comment.isSolution) {
                    SolarCoral.copy(alpha = 0.08f)
                } else {
                    CosmicDark
                }
            ),
            shape = RoundedCornerShape(12.dp),
            border = if (comment.isSolution) {
                BorderStroke(1.5.dp, SolarCoral.copy(alpha = 0.6f))
            } else if (depth > 0) {
                BorderStroke(1.dp, CosmicGray)
            } else {
                BorderStroke(1.dp, CosmicGray.copy(alpha = 0.5f))
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Comment Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(generateAvatarBrush(comment.avatarSeed), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = comment.avatarSeed,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (comment.author == "You") "You" else "@${comment.author}",
                            color = HeaderText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "• ${formatTimeAgo(comment.timestamp)}",
                            color = SoftText,
                            fontSize = 11.sp
                        )
                    }

                    // Actionable Solution badge
                    if (comment.isSolution) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SolarCoral.copy(alpha = 0.2f))
                                .border(1.dp, SolarCoral, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = SolarCoral,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ACTIONABLE FIX",
                                    color = SolarCoral,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Comment Content Body
                Text(
                    text = comment.content,
                    color = HeaderText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Comment Actions: Upvote, Downvote, and Nested Reply Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voting controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onUpvote(comment.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Upvote comment",
                                tint = SoftText,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "${comment.upvotesCount - comment.downvotesCount}",
                            color = SoftText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )

                        IconButton(
                            onClick = { onDownvote(comment.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Downvote comment",
                                tint = SoftText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Reply Action button
                    TextButton(
                        onClick = { onReply(comment) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = NeoCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reply",
                            color = NeoCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- RECURSIVE STEP ---
        // Render child comments recursively under this node with indentation & line connector
        if (childComments.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                // Visual thread guide connector line
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .background(NeoCyan.copy(alpha = 0.2f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    childComments.forEach { child ->
                        RecursiveCommentNode(
                            comment = child,
                            allComments = allComments,
                            depth = depth + 1,
                            maxDepth = maxDepth,
                            onReply = onReply,
                            onUpvote = onUpvote,
                            onDownvote = onDownvote
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty state card when no comments have been posted yet.
 */
@Composable
private fun EmptyCommentsCard(onActionableClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CosmicDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = SoftText,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No responses in this discussion yet",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Be the first to provide actionable advice or propose a solution!",
                color = SoftText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onActionableClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SolarCoral,
                    contentColor = CosmicBlack
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "💡 Contribute Actionable Solution",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Dialog to contribute an actionable solution with step-by-step guidance to problem solving.
 */
@Composable
private fun ContributeActionableSolutionDialog(
    discussionTitle: String,
    onDismiss: () -> Unit,
    onSubmit: (title: String, steps: String, outcome: String) -> Unit
) {
    var solutionTitle by remember { mutableStateOf("") }
    var actionSteps by remember { mutableStateOf("") }
    var expectedOutcome by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(16.dp),
            color = CosmicDark,
            border = BorderStroke(1.dp, SolarCoral.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(SolarCoral, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = CosmicBlack,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Contribute Solution",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SoftText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Solving: \"$discussionTitle\"",
                    color = SoftText,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Solution Title Input
                Text(
                    text = "Solution Approach / Title",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = solutionTitle,
                    onValueChange = { solutionTitle = it },
                    placeholder = {
                        Text(
                            "e.g., Epoch-based Watermarking & Delta Catchup",
                            color = SoftText,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("actionable_title_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SolarCoral,
                        unfocusedBorderColor = CosmicGray,
                        focusedContainerColor = CosmicInput,
                        unfocusedContainerColor = CosmicInput,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action Steps Input
                Text(
                    text = "Step-by-Step Action Plan",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = actionSteps,
                    onValueChange = { actionSteps = it },
                    placeholder = {
                        Text(
                            "1. First step...\n2. Implementation details...\n3. Verification...",
                            color = SoftText,
                            fontSize = 13.sp
                        )
                    },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("actionable_steps_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SolarCoral,
                        unfocusedBorderColor = CosmicGray,
                        focusedContainerColor = CosmicInput,
                        unfocusedContainerColor = CosmicInput,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Expected Outcome Input
                Text(
                    text = "Expected Outcome & Feasibility",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = expectedOutcome,
                    onValueChange = { expectedOutcome = it },
                    placeholder = {
                        Text(
                            "Reduces p99 latency by ~70% and bounds memory overhead.",
                            color = SoftText,
                            fontSize = 13.sp
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("actionable_outcome_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SolarCoral,
                        unfocusedBorderColor = CosmicGray,
                        focusedContainerColor = CosmicInput,
                        unfocusedContainerColor = CosmicInput,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (solutionTitle.trim().isNotEmpty()) {
                            onSubmit(solutionTitle, actionSteps, expectedOutcome)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SolarCoral,
                        contentColor = CosmicBlack
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_actionable_solution_btn"),
                    enabled = solutionTitle.trim().isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Publish Actionable Solution (+25 Rep)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
