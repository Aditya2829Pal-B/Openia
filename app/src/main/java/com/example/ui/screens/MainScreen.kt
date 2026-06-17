package com.example.ui.screens

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.LoadState
import com.example.data.model.CommentEntity
import com.example.data.model.PostEntity
import com.example.ui.viewmodel.PostViewModel
import com.example.core.error.GlobalErrorHandler
import kotlinx.coroutines.flow.collectLatest
import com.example.ui.components.AdvancedProfileCard
import com.example.ui.components.ReputationGlowingIndicator
import java.text.SimpleDateFormat
import java.util.*

// Custom Cosmic Color Palette
val CosmicBlack = Color(0xFF0B0E14)
val CosmicDark = Color(0xFF131722)
val CosmicGray = Color(0xFF1E2433)
val CosmicInput = Color(0xFF161A24)
val NeoCyan = Color(0xFF00E5FF)
val SolarCoral = Color(0xFFFF5722)
val SoftText = Color(0xFF90A4AE)
val SoftBorder = Color(0xFF37474F)
val HeaderText = Color(0xFFECEFF1)
val AccentPurple = Color(0xFF7C4DFF)
val NeoRed = Color(0xFFFF1744)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PostViewModel) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val pagedPosts = viewModel.pagedPosts.collectAsLazyPagingItems()
    val currentPost by viewModel.currentPost.collectAsStateWithLifecycle()
    val currentComments by viewModel.currentComments.collectAsStateWithLifecycle()
    val reactions by viewModel.currentReactions.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()

    val allFollows by viewModel.allFollows.collectAsStateWithLifecycle()
    val authorReputations by viewModel.authorReputations.collectAsStateWithLifecycle()
    val advancedReputations by viewModel.advancedReputations.collectAsStateWithLifecycle()
    val myProfile by viewModel.myProfile.collectAsStateWithLifecycle()
    val drafts by viewModel.drafts.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showIntelligenceMap by remember { mutableStateOf(false) }
    var activeNavTab by remember { mutableStateOf(0) } // 0 = Feed, 1 = My Profile
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        GlobalErrorHandler.errorEvents.collectLatest { event ->
            val message = event.error.message ?: "An unexpected error occurred"
            val actionLabel = if (event.retryAction != null) "Retry" else null
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = androidx.compose.material3.SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                event.retryAction?.invoke()
            }
        }
    }

    var draftImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        draftImageUri = uri
    }

    // Background gradient canvas
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CosmicBlack, CosmicDark, CosmicBlack)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Dynamic Tab Views
            androidx.compose.animation.AnimatedContent(
                targetState = activeNavTab,
                label = "Tab Transition",
                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    0 -> {
                        // FEED VIEW LAYOUT
                        val followedOnlyFlag by viewModel.followedOnly.collectAsStateWithLifecycle()
                        
                        PullToRefreshBox(
                            isRefreshing = isRefreshing || pagedPosts.loadState.refresh is LoadState.Loading,
                            onRefresh = { 
                                viewModel.refreshFeed()
                                pagedPosts.refresh()
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (pagedPosts.loadState.refresh is LoadState.Error) {
                                Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        AppHeader(
                                            onSearchChanged = { viewModel.setSearchQuery(it) },
                                            searchQuery = searchQuery,
                                            activeSort = selectedSort,
                                            onSortToggled = {
                                                val nextSort = if (selectedSort == "TRENDING") "LATEST" else "TRENDING"
                                                viewModel.setSortOrder(nextSort)
                                            },
                                            onMapToggled = { showIntelligenceMap = true }
                                        )
                                        Text("Error loading data", color = Color.White)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(onClick = { pagedPosts.retry() }) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    item {
                                        Column {
                                            AppHeader(
                                                onSearchChanged = { viewModel.setSearchQuery(it) },
                                                searchQuery = searchQuery,
                                                activeSort = selectedSort,
                                                onSortToggled = {
                                                    val nextSort = if (selectedSort == "TRENDING") "LATEST" else "TRENDING"
                                                    viewModel.setSortOrder(nextSort)
                                                },
                                                onMapToggled = { showIntelligenceMap = true }
                                            )
                                            FeedTogglesAndCategories(
                                                selectedType = selectedType,
                                                onTypeSelected = { viewModel.setFilterType(it) },
                                                selectedCategory = selectedCategory,
                                                onCategorySelected = { viewModel.setFilterCategory(it) }
                                            )
                                            
                                            // Following Only Filter Toggle Row
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.setFollowedOnly(!followedOnlyFlag) }
                                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Favorite,
                                                        contentDescription = null,
                                                        tint = if (followedOnlyFlag) NeoCyan else SoftText,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Filter feed by Followed Thinkers Only",
                                                        color = if (followedOnlyFlag) Color.White else SoftText,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Switch(
                                                    checked = followedOnlyFlag,
                                                    onCheckedChange = { viewModel.setFollowedOnly(it) },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = NeoCyan,
                                                        checkedTrackColor = NeoCyan.copy(alpha = 0.3f),
                                                        uncheckedThumbColor = CosmicGray,
                                                        uncheckedTrackColor = CosmicInput
                                                    ),
                                                    modifier = Modifier.testTag("following_only_switch")
                                                )
                                            }
                                        }
                                    }
                                    
                                    if (pagedPosts.itemCount == 0 && pagedPosts.loadState.refresh is LoadState.NotLoading) {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                                EmptyStateView(searchQuery, selectedCategory, selectedType)
                                            }
                                        }
                                    } else {
                                        item {
                                            Box(modifier = Modifier.padding(16.dp)) {
                                                TrendSummaryHeader(posts, selectedType)
                                            }
                                        }

                                        items(
                                            count = pagedPosts.itemCount,
                                            key = { index -> pagedPosts[index]?.id ?: "placeholder_$index" }
                                        ) { index ->
                                            val post = pagedPosts[index]
                                            if (post != null) {
                                                Box(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                                                    PostCard(
                                                        post = post,
                                                        followedAuthors = allFollows,
                                                        reputations = authorReputations,
                                                        advancedReputations = advancedReputations,
                                                        isBookmarked = bookmarks.any { it.postId == post.id },
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
                                        }
                                        
                                        if (pagedPosts.loadState.append is LoadState.Loading) {
                                            item {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(color = NeoCyan)
                                                }
                                            }
                                        } else if (pagedPosts.loadState.append is LoadState.Error) {
                                            item {
                                                Text("Error loading more items. Tap to retry.", color = Color.Red, modifier = Modifier.clickable { pagedPosts.retry() })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            AppHeader(
                                onSearchChanged = { viewModel.setSearchQuery(it) },
                                searchQuery = searchQuery,
                                activeSort = selectedSort,
                                onSortToggled = {
                                    val nextSort = if (selectedSort == "TRENDING") "LATEST" else "TRENDING"
                                    viewModel.setSortOrder(nextSort)
                                },
                                onMapToggled = { showIntelligenceMap = true }
                            )
                            Box(modifier = Modifier.fillMaxSize()) {
                                CommunityTabContent(
                                    viewModel = viewModel,
                                    posts = posts,
                                    reputations = authorReputations,
                                    advancedReputations = advancedReputations
                                )
                            }
                        }
                    }
                    2 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            AppHeader(
                                onSearchChanged = { viewModel.setSearchQuery(it) },
                                searchQuery = searchQuery,
                                activeSort = selectedSort,
                                onSortToggled = {
                                    val nextSort = if (selectedSort == "TRENDING") "LATEST" else "TRENDING"
                                    viewModel.setSortOrder(nextSort)
                                },
                                onMapToggled = { showIntelligenceMap = true }
                            )
                            Box(modifier = Modifier.fillMaxSize()) {
                                CategoriesTabContent(
                                    viewModel = viewModel,
                                    onCategorySelected = {
                                        viewModel.setFilterCategory(it)
                                        activeNavTab = 0
                                    }
                                )
                            }
                        }
                    }
                    3 -> {
                        // PROFILE TAB LAYOUT
                        Column(modifier = Modifier.fillMaxSize()) {
                            AppHeader(
                                onSearchChanged = { viewModel.setSearchQuery(it) },
                                searchQuery = searchQuery,
                                activeSort = selectedSort,
                                onSortToggled = {
                                    val nextSort = if (selectedSort == "TRENDING") "LATEST" else "TRENDING"
                                    viewModel.setSortOrder(nextSort)
                                },
                                onMapToggled = { showIntelligenceMap = true }
                            )
                            Box(modifier = Modifier.fillMaxSize()) {
                                ProfileTabContent(
                                viewModel = viewModel,
                                myProfile = myProfile,
                                allFollows = allFollows,
                                reputations = authorReputations,
                                advancedReputations = advancedReputations,
                                posts = posts
                            )
                        }
                    } // closes Column
                    } // closes 3 -> block
                } // closes when
            } // closes AnimatedContent

            // Bottom Navigation tabs bar
            NavigationBar(
                containerColor = CosmicDark,
                modifier = Modifier.height(72.dp)
            ) {
                NavigationBarItem(
                    selected = activeNavTab == 0,
                    onClick = { activeNavTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Feed Tab",
                            tint = if (activeNavTab == 0) NeoCyan else SoftText
                        )
                    },
                    label = {
                        Text(
                            text = "Feed",
                            color = if (activeNavTab == 0) NeoCyan else SoftText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = CosmicGray
                    ),
                    modifier = Modifier.testTag("nav_feed_tab")
                )

                NavigationBarItem(
                    selected = activeNavTab == 1,
                    onClick = { activeNavTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Community Tab",
                            tint = if (activeNavTab == 1) SolarCoral else SoftText
                        )
                    },
                    label = {
                        Text(
                            text = "Community",
                            color = if (activeNavTab == 1) SolarCoral else SoftText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = CosmicGray
                    ),
                    modifier = Modifier.testTag("nav_community_tab")
                )

                NavigationBarItem(
                    selected = activeNavTab == 2,
                    onClick = { activeNavTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Categories Tab",
                            tint = if (activeNavTab == 2) Color(0xFF64B5F6) else SoftText
                        )
                    },
                    label = {
                        Text(
                            text = "Topics",
                            color = if (activeNavTab == 2) Color(0xFF64B5F6) else SoftText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = CosmicGray
                    ),
                    modifier = Modifier.testTag("nav_categories_tab")
                )

                NavigationBarItem(
                    selected = activeNavTab == 3,
                    onClick = { activeNavTab = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Tab",
                            tint = if (activeNavTab == 3) AccentPurple else SoftText
                        )
                    },
                    label = {
                        Text(
                            text = "Profile",
                            color = if (activeNavTab == 3) AccentPurple else SoftText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = CosmicGray
                    ),
                    modifier = Modifier.testTag("nav_profile_tab")
                )
            }
        }

        // Floating Action Button for post creation (only active on Feed tab)
        if (activeNavTab == 0) {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .padding(bottom = 76.dp) // Keeps button safely stacked above NavigationBar
                    .testTag("create_post_btn")
                    .clip(RoundedCornerShape(16.dp)),
                containerColor = AccentPurple,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create New post",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Sliding Details Dialog Overlay for selected Post detail
        currentPost?.let { post ->
            BackHandler {
                viewModel.selectPost(null)
            }
            PostDetailsOverlay(
                post = post,
                comments = currentComments,
                reactions = reactions,
                isAnalyzing = isAnalyzing,
                followedAuthors = allFollows,
                reputations = authorReputations,
                advancedReputations = advancedReputations,
                onFollowToggle = { viewModel.toggleFollow(it) },
                onClose = { viewModel.selectPost(null) },
                onAddComment = { userComment, isSol, parentCommentId ->
                    viewModel.submitComment(post.id, userComment, isSolution = isSol, parentCommentId = parentCommentId)
                },
                onTriggerAi = { viewModel.analyzeWithGemini(post.id) },
                onAgreeClick = { viewModel.agreePost(post.id) },
                onDisagreeClick = { viewModel.disagreePost(post.id) },
                onUpvoteClick = { viewModel.upvotePost(post.id) },
                onDownvoteClick = { viewModel.downvotePost(post.id) },
                onEmpathyClick = { viewModel.empathyPost(post.id) },
                onCommentUpvoteClick = { viewModel.upvoteComment(it) },
                onCommentDownvoteClick = { viewModel.downvoteComment(it) }
            )
        }

        // Creation Dialog Overlay
        if (showCreateDialog) {
            CreatePostDialog(
                selectedImageUri = draftImageUri,
                onPickImage = { imagePickerLauncher.launch("image/*") },
                onClearImage = { draftImageUri = null },
                onDismiss = { 
                    showCreateDialog = false
                    draftImageUri = null
                },
                onPublish = { title, content, type, cat, tags, imageUri ->
                    viewModel.publishPost(title, content, type, cat, tags, imageUri)
                    showCreateDialog = false
                    draftImageUri = null
                },
                onSaveDraft = { title, content, type, cat ->
                    viewModel.saveDraft(title, content, type, cat)
                    showCreateDialog = false
                    draftImageUri = null
                }
            )
        }

        // Global Intelligence Map Overlay
        if (showIntelligenceMap) {
            Dialog(
                onDismissRequest = { showIntelligenceMap = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                GlobalIntelligenceMapOverlay(onClose = { showIntelligenceMap = false })
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 60.dp)
        )
    }
}

@Composable
fun AppHeader(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    activeSort: String,
    onSortToggled: () -> Unit,
    onMapToggled: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CosmicBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Stylish custom logo icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Brush.linearGradient(listOf(NeoCyan, SolarCoral)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ω",
                        color = CosmicBlack,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Openia",
                    color = HeaderText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Map Toggle Button
                IconButton(
                    onClick = onMapToggled,
                    modifier = Modifier
                        .background(CosmicGray, CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Global Intelligence Map",
                        tint = NeoCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Sort Toggle Button
                IconButton(
                    onClick = onSortToggled,
                    modifier = Modifier
                        .background(CosmicGray, CircleShape)
                        .size(40.dp)
                        .testTag("sort_toggle_button")
                ) {
                    Icon(
                        imageVector = if (activeSort == "TRENDING") Icons.Default.Star else Icons.Default.Refresh,
                        contentDescription = "Toggle Sort Mode",
                        tint = if (activeSort == "TRENDING") SolarCoral else NeoCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar with Material 3 styling
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_field"),
            placeholder = { Text("Search opinions, problems, tags...", color = SoftText) },
            prefix = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = SoftText,
                    modifier = Modifier.padding(end = 8.dp)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPurple,
                unfocusedBorderColor = CosmicGray,
                focusedContainerColor = CosmicInput,
                unfocusedContainerColor = CosmicInput,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
}

@Composable
fun FeedTogglesAndCategories(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("ALL", "Tech", "Work", "Climate", "Society", "Relationships", "Philosophy")
    val types = listOf(
        "ALL" to "All",
        "OPINION" to "Opinions",
        "PROBLEM" to "Problems"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CosmicBlack)
            .padding(bottom = 8.dp)
    ) {
        // Feed Type tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            types.forEach { (typeVal, label) ->
                val isActive = selectedType == typeVal
                val activeBrush = when (typeVal) {
                    "OPINION" -> Brush.horizontalGradient(listOf(NeoCyan, Color(0xFF00B0FF)))
                    "PROBLEM" -> Brush.horizontalGradient(listOf(SolarCoral, Color(0xFFFF8A80)))
                    else -> Brush.horizontalGradient(listOf(AccentPurple, Color(0xFFB388FF)))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) Color.Transparent else CosmicInput)
                        .then(
                            if (isActive) Modifier.background(activeBrush)
                            else Modifier.border(1.dp, CosmicGray, RoundedCornerShape(8.dp))
                        )
                        .clickable { onTypeSelected(typeVal) }
                        .padding(vertical = 10.dp)
                        .testTag("feed_tab_$typeVal"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isActive) CosmicBlack else HeaderText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Categories Lazy Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) AccentPurple else CosmicGray)
                        .border(
                            1.dp,
                            if (isSelected) AccentPurple else CosmicGray,
                            CircleShape
                        )
                        .clickable { onCategorySelected(category) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("category_pill_$category"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun TrendSummaryHeader(posts: List<PostEntity>, currentTypeFilter: String) {
    if (posts.isEmpty()) return

    val opinions = posts.filter { it.postType == "OPINION" }
    val problems = posts.filter { it.postType == "PROBLEM" }

    val summaryText = when (currentTypeFilter) {
        "OPINION" -> "Viewing ${opinions.size} opinions. Over ${opinions.sumOf { it.agreeCount + it.disagreeCount }} members debating."
        "PROBLEM" -> "Viewing ${problems.size} active problems. Solidarity state: ${problems.sumOf { it.empathyCount }} feel this."
        else -> "${posts.size} threads live. Trending topics in Tech, Policy & Mindsets."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trend_summary_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicGray.copy(alpha = 0.5f))
    ) {
        Column {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (currentTypeFilter == "PROBLEM") SolarCoral else NeoCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = summaryText,
                    color = SoftText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            HorizontalDivider(color = CosmicDark)
            D3SentimentTrendChart()
        }
    }
}

@Composable
fun D3SentimentTrendChart() {
    val sentimentPoints = listOf(0.4f, 0.6f, 0.3f, 0.7f, 0.5f, 0.8f, 0.9f)
    val labels = listOf("-24h", "-20h", "-16h", "-12h", "-8h", "-4h", "Now")
    val gradientColors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.5f), Color.Transparent) // Cosmic Purple to transparent
    val lineColor = Color(0xFF10B981) // Emerald Green

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("D3-Style Trend Synthesis: Public Sentiment (24h)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas

            val maxPoint = sentimentPoints.maxOrNull() ?: 1f
            val minPoint = sentimentPoints.minOrNull() ?: 0f
            val range = (maxPoint - minPoint).coerceAtLeast(0.1f)
            
            val pointSpacing = size.width / (sentimentPoints.size - 1)
            
            val path = androidx.compose.ui.graphics.Path()
            val fillPath = androidx.compose.ui.graphics.Path()

            val points = sentimentPoints.mapIndexed { index, value ->
                val x = index * pointSpacing
                val normalizedY = 1f - ((value - minPoint) / range)
                val y = (normalizedY * size.height).coerceIn(0f, size.height)
                androidx.compose.ui.geometry.Offset(x, y)
            }

            // Move to first point
            path.moveTo(points.first().x, points.first().y)
            fillPath.moveTo(points.first().x, size.height) // start from bottom
            fillPath.lineTo(points.first().x, points.first().y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                
                // Using Cubic Bezier for smooth curves (D3 style smooth spline)
                val controlX1 = p0.x + (pointSpacing / 2f)
                val controlY1 = p0.y
                val controlX2 = p0.x + (pointSpacing / 2f)
                val controlY2 = p1.y

                path.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
            }

            fillPath.lineTo(points.last().x, size.height)
            fillPath.close()

            // Draw Area (Gradient)
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = gradientColors,
                    startY = 0f,
                    endY = size.height
                )
            )

            // Draw Line
            drawPath(
                path = path,
                color = lineColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )

            // Draw Points
            points.forEach { point ->
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = lineColor,
                    radius = 2.5.dp.toPx(),
                    center = point
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // X-axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(text = label, color = SoftText, fontSize = 9.sp)
            }
        }
    }
}

fun getReputationText(points: Int): String {
    return when {
        points < 10 -> "Novice"
        points in 10..30 -> "Credible Peer"
        points in 31..70 -> "Insightful Mentor"
        points in 71..150 -> "Consensus Analyst"
        else -> "Legendary Citizen"
    }
}

fun getReputationColor(points: Int): Color {
    return when {
        points < 10 -> SoftText
        points in 10..30 -> NeoCyan
        points in 31..70 -> AccentPurple
        points in 71..150 -> Color(0xFFFFD700)
        else -> SolarCoral
    }
}

@Composable
fun PostCard(
    post: PostEntity,
    followedAuthors: List<String> = emptyList(),
    reputations: Map<String, Int> = emptyMap(),
    advancedReputations: Map<String, com.example.domain.model.AdvancedReputation> = emptyMap(),
    isBookmarked: Boolean = false,
    onFollowToggle: ((String) -> Unit)? = null,
    onBookmarkToggle: (() -> Unit)? = null,
    onCardClick: () -> Unit,
    onAgreeClick: () -> Unit,
    onDisagreeClick: () -> Unit,
    onUpvoteClick: () -> Unit,
    onDownvoteClick: () -> Unit,
    onEmpathyClick: () -> Unit
) {
    val isOpinion = post.postType == "OPINION"
    val accentColor = if (isOpinion) NeoCyan else SolarCoral

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Top Meta (Author, Date, Category Tag)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Custom Profile Letter Avatar
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                generateAvatarBrush(post.avatarSeed),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.avatarSeed,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (post.author == "You") "You" else "@${post.author}",
                                color = HeaderText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (post.author != "You") {
                                Spacer(modifier = Modifier.width(6.dp))
                                val isFollowing = followedAuthors.contains(post.author)
                                Text(
                                    text = if (isFollowing) "• Following" else "• Follow",
                                    color = if (isFollowing) AccentPurple else NeoCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .clickable { onFollowToggle?.invoke(post.author) }
                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                        .testTag("follow_toggle_btn_${post.author}")
                                )
                            }
                        }

                        // Reputation display
                        val reputation = reputations[post.author] ?: 0
                        val advancedRep = advancedReputations[post.author]
                        com.example.ui.components.ContributorBadge(advancedRep, reputation)

                        Text(
                            text = formatTimeAgo(post.timestamp),
                            color = SoftText,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category Tag Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = post.category,
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (onBookmarkToggle != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) AccentPurple else SoftText,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onBookmarkToggle() }
                                .padding(2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body content
            Text(
                text = post.title,
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = post.content,
                color = SoftText,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (post.imageUri != null) {
                Spacer(modifier = Modifier.height(12.dp))
                coil.compose.AsyncImage(
                    model = post.imageUri,
                    contentDescription = "Post Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            // Tags display
            if (post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.wrapContentHeight()
                ) {
                    post.tags.split(",").forEach { tag ->
                        Text(
                            text = "#$tag",
                            color = accentColor.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Split interaction states based on Post Type
            if (isOpinion) {
                // OPINION -> Agree/Disagree debate metrics
                OpinionMetricsBar(
                    agreeVal = post.agreeCount,
                    disagreeVal = post.disagreeCount
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Agree Action Button
                        Button(
                            onClick = onAgreeClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CosmicDark,
                                contentColor = NeoCyan
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .border(1.dp, NeoCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .testTag("agree_btn_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Agree (${post.agreeCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Disagree Action Button
                        Button(
                            onClick = onDisagreeClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CosmicDark,
                                contentColor = SolarCoral
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .border(1.dp, SolarCoral.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .testTag("disagree_btn_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Disagree (${post.disagreeCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Comments Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = SoftText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.commentCount} debates",
                            color = SoftText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // PROBLEM -> Openia community issue resolution + Empathy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Up/Down Vote Controls
                    Row(
                        modifier = Modifier
                            .background(CosmicDark, RoundedCornerShape(8.dp))
                            .border(1.dp, CosmicGray, RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onUpvoteClick,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("upvote_btn_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Upvote",
                                tint = SolarCoral,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "${post.upvotesCount - post.downvotesCount}",
                            color = HeaderText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = onDownvoteClick,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("downvote_btn_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Downvote",
                                tint = SoftText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Empathy Action "I feel this too"
                        Button(
                            onClick = onEmpathyClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CosmicDark,
                                contentColor = SolarCoral
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .border(1.dp, SolarCoral.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .testTag("empathy_btn_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = SolarCoral
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("I deal with this (${post.empathyCount})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Comments / Solutions offered Count
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(CosmicDark, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = SoftText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${post.commentCount} advice", color = SoftText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OpinionMetricsBar(agreeVal: Int, disagreeVal: Int) {
    val total = agreeVal + disagreeVal
    val agreeRatio = if (total == 0) 0.5f else agreeVal.toFloat() / total.toFloat()
    val disagreeRatio = 1f - agreeRatio

    val agreePercent = (agreeRatio * 100).toInt()
    val disagreePercent = 100 - agreePercent

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Agreeing: $agreePercent%", color = NeoCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Disagreeing: $disagreePercent%", color = SolarCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(CosmicDark)
        ) {
            // Agree filling
            if (agreeRatio > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(agreeRatio)
                        .background(
                            Brush.horizontalGradient(listOf(NeoCyan, Color(0xFF00B0FF)))
                        )
                )
            }
            // Disagree filling
            if (disagreeRatio > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(disagreeRatio)
                        .background(
                            Brush.horizontalGradient(listOf(SolarCoral, Color(0xFFFF8A80)))
                        )
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(query: String, cat: String, type: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp)
            .testTag("empty_state_view"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = SoftText,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No open debates or problems found",
                color = HeaderText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Be the first to share your hot take or a frustrating problem in category '$cat' to spark an open discussion!",
                color = SoftText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

// Full Slide Details Overlay Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsOverlay(
    post: PostEntity,
    comments: List<CommentEntity>,
    reactions: List<String>,
    isAnalyzing: Boolean,
    followedAuthors: List<String> = emptyList(),
    reputations: Map<String, Int> = emptyMap(),
    advancedReputations: Map<String, com.example.domain.model.AdvancedReputation> = emptyMap(),
    onFollowToggle: ((String) -> Unit)? = null,
    onClose: () -> Unit,
    onAddComment: (String, Boolean, Int?) -> Unit,
    onTriggerAi: () -> Unit,
    onAgreeClick: () -> Unit,
    onDisagreeClick: () -> Unit,
    onUpvoteClick: () -> Unit,
    onDownvoteClick: () -> Unit,
    onEmpathyClick: () -> Unit,
    onCommentUpvoteClick: (Int) -> Unit = {},
    onCommentDownvoteClick: (Int) -> Unit = {}
) {
    var userCommentText by remember { mutableStateOf("") }
    var offerAsSolution by remember { mutableStateOf(false) }
    var replyingToCommentId by remember { mutableStateOf<Int?>(null) }
    val isOpinion = post.postType == "OPINION"
    val accentColor = if (isOpinion) NeoCyan else SolarCoral

    val flatComments = remember(comments) {
        fun flatten(parentId: Int?, depth: Int): List<Pair<CommentEntity, Int>> {
            val result = mutableListOf<Pair<CommentEntity, Int>>()
            comments.filter { it.parentCommentId == parentId }.sortedBy { it.timestamp }.forEach { child ->
                result.add(Pair(child, depth))
                result.addAll(flatten(child.id, depth + 1))
            }
            return result
        }
        flatten(null, 0)
    }

    // Fully modal design so users can deeply explore opinions & problems
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBlack),
            color = CosmicBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Control Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmicBlack)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .background(CosmicGray, CircleShape)
                            .size(40.dp)
                            .testTag("close_details_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Go back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = if (isOpinion) "Opinion Debate" else "Problem Advice",
                        color = HeaderText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Refresh AI action if already analyzed, otherwise dynamic tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = post.category,
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Scrolling thread content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        com.example.ui.components.EvolutionLineageBanner(
                            post = post, 
                            onEvolveClick = { /* Implementation to evolve idea */ }
                        )
                    }
                    
                    item {
                        // Core Thread Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicGray),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                generateAvatarBrush(post.avatarSeed),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = post.avatarSeed,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (post.author == "You") "You" else "@${post.author}",
                                                color = HeaderText,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (post.author != "You") {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                val isFollowing = followedAuthors.contains(post.author)
                                                Text(
                                                    text = if (isFollowing) "• Following" else "• Follow",
                                                    color = if (isFollowing) AccentPurple else NeoCyan,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clickable { onFollowToggle?.invoke(post.author) }
                                                        .padding(4.dp)
                                                        .testTag("detail_follow_btn_${post.author}")
                                                )
                                            }
                                        }

                                        // Author reputation indicator
                                        val reputation = reputations[post.author] ?: 0
                                        val badge = getReputationText(reputation)
                                        val badgeColor = getReputationColor(reputation)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = badgeColor,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "$badge ($reputation rep)",
                                                color = badgeColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Text(
                                            text = formatFullDate(post.timestamp),
                                            color = SoftText,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = post.title,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 28.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = post.content,
                                    color = HeaderText.copy(alpha = 0.9f),
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )

                                if (post.imageUri != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    coil.compose.AsyncImage(
                                        model = post.imageUri,
                                        contentDescription = "Post Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                                    )
                                }

                                if (post.tags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                                    androidx.compose.foundation.layout.FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        post.tags.split(",").forEach { tag ->
                                            Text(
                                                text = "#$tag",
                                                color = accentColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Inline interaction indicators
                                if (isOpinion) {
                                    OpinionMetricsBar(agreeVal = post.agreeCount, disagreeVal = post.disagreeCount)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = onAgreeClick,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (reactions.contains("AGREE")) NeoCyan else CosmicDark,
                                                contentColor = if (reactions.contains("AGREE")) CosmicBlack else NeoCyan
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).testTag("detail_agree_btn")
                                        ) {
                                            Text("Agree (${post.agreeCount})", fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = onDisagreeClick,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (reactions.contains("DISAGREE")) SolarCoral else CosmicDark,
                                                contentColor = if (reactions.contains("DISAGREE")) CosmicBlack else SolarCoral
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).testTag("detail_disagree_btn")
                                        ) {
                                            Text("Disagree (${post.disagreeCount})", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .background(CosmicDark, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = onUpvoteClick) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = null,
                                                    tint = if (reactions.contains("UPVOTE")) SolarCoral else SoftText
                                                )
                                            }
                                            Text(
                                                text = "${post.upvotesCount - post.downvotesCount}",
                                                color = HeaderText,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(onClick = onDownvoteClick) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = if (reactions.contains("DOWNVOTE")) SolarCoral else SoftText
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = onEmpathyClick,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (reactions.contains("EMPATHY")) SolarCoral else CosmicDark,
                                                contentColor = if (reactions.contains("EMPATHY")) CosmicBlack else SolarCoral
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("detail_empathy_btn")
                                        ) {
                                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("I Deal With This (${post.empathyCount})")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        com.example.ui.components.ConsensusIntelligenceCard(post = post)
                    }

                    // --- Dynamic AI Gemini Synthesis Section ---
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicDark),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    BorderStroke(
                                        1.5.dp,
                                        Brush.linearGradient(listOf(NeoCyan, SolarCoral, AccentPurple))
                                    ),
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    Brush.linearGradient(listOf(NeoCyan, SolarCoral)),
                                                    CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Gemini AI Synth",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Button(
                                        onClick = onTriggerAi,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AccentPurple,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp).testTag("trigger_ai_btn")
                                    ) {
                                        if (isAnalyzing) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = if (post.aiSummary != null) "Recalculate" else "Synthesize",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (isAnalyzing) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = accentColor, strokeWidth = 3.dp)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("Synthesizing Thread Data...", color = SoftText, fontSize = 14.sp)
                                        }
                                    }
                                } else if (post.aiSummary == null) {
                                    Text(
                                        text = "Generate an advanced AI multi-perspective breakdown. For Problems: Root Cause, Actionable Solutions & Empathy. For Opinions: Strengths & Blindspots.",
                                        color = SoftText,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    // Custom Tabs for AI outputs
                                    var activeAiTab by remember { mutableStateOf(0) }
                                    val aiTabsList = if (isOpinion) {
                                        listOf("Strengths", "Debate Extraction", "Knowledge Card")
                                    } else {
                                        listOf("Root Cause", "Actionable Plan", "Knowledge Card")
                                    }

                                    ScrollableTabRow(
                                        selectedTabIndex = activeAiTab,
                                        containerColor = Color.Transparent,
                                        contentColor = accentColor,
                                        edgePadding = 0.dp,
                                        divider = {}
                                    ) {
                                        aiTabsList.forEachIndexed { i, title ->
                                            Tab(
                                                selected = activeAiTab == i,
                                                onClick = { activeAiTab = i },
                                                text = {
                                                    Text(
                                                        title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = if (activeAiTab == i) Color.White else SoftText
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    val activeContent = when (activeAiTab) {
                                        0 -> post.aiSummary ?: ""
                                        1 -> post.aiSolutions ?: ""
                                        else -> post.aiConsensus ?: "" // Contains Knowledge Card
                                    }

                                    Text(
                                        text = activeContent,
                                        color = HeaderText,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // --- Comments List Heading ---
                    item {
                        Text(
                            text = if (isOpinion) "Debate replies (${comments.size})" else "Actionable advice / Solutions (${comments.size})",
                            color = HeaderText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Comments content
                    if (flatComments.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CosmicDark, RoundedCornerShape(12.dp))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isOpinion) "No debates generated yet. Frame your critique below!" else "No advice yet. Offer your advice or actionable solution below!",
                                    color = SoftText,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(flatComments, key = { it.first.id }) { (comment, depth) ->
                            Box(modifier = Modifier.padding(start = (depth * 20).dp).padding(bottom = 8.dp)) {
                                CommentRow(
                                    comment = comment,
                                    isOpinion = isOpinion,
                                    reputations = reputations,
                                    advancedReputations = advancedReputations,
                                    onReplyClick = { replyingToCommentId = comment.id },
                                    onUpvoteClick = { onCommentUpvoteClick(comment.id) },
                                    onDownvoteClick = { onCommentDownvoteClick(comment.id) }
                                )
                            }
                        }
                    }
                }

                // Add Comment Row (persistent bottom input)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CosmicDark,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (replyingToCommentId != null) {
                            val targetComment = comments.find { it.id == replyingToCommentId }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Replying to @${targetComment?.author ?: "Unknown"}",
                                    color = AccentPurple,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { replyingToCommentId = null }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, null, tint = SoftText, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        if (!isOpinion) {
                            // Openia community option to mark comment specifically as a "Solution"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = offerAsSolution,
                                    onCheckedChange = { offerAsSolution = it },
                                    colors = CheckboxDefaults.colors(checkedColor = SolarCoral)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Mark as an Actionable Solution / Workaround",
                                    color = HeaderText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = userCommentText,
                                onValueChange = { userCommentText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("comment_input_field"),
                                placeholder = {
                                    Text(
                                        text = if (isOpinion) "Add your debate argument..." else "Offer a solution or supportive advice...",
                                        color = SoftText,
                                        fontSize = 13.sp
                                    )
                                },
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = CosmicGray,
                                    focusedContainerColor = CosmicInput,
                                    unfocusedContainerColor = CosmicInput,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (userCommentText.trim().isNotEmpty()) {
                                        onAddComment(userCommentText, offerAsSolution, replyingToCommentId)
                                        userCommentText = ""
                                        offerAsSolution = false
                                        replyingToCommentId = null
                                    }
                                },
                                modifier = Modifier
                                    .background(accentColor, CircleShape)
                                    .size(44.dp)
                                    .testTag("send_comment_btn"),
                                enabled = userCommentText.trim().isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Post debate",
                                    tint = CosmicBlack,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentRow(
    comment: CommentEntity, 
    isOpinion: Boolean, 
    reputations: Map<String, Int> = emptyMap(),
    advancedReputations: Map<String, com.example.domain.model.AdvancedReputation> = emptyMap(),
    onReplyClick: () -> Unit,
    onUpvoteClick: () -> Unit,
    onDownvoteClick: () -> Unit
) {
    val highlightColor = if (isOpinion) NeoCyan else SolarCoral

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (comment.isSolution) highlightColor.copy(alpha = 0.08f) else CosmicDark
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (comment.isSolution) BorderStroke(1.dp, highlightColor.copy(alpha = 0.3f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                generateAvatarBrush(comment.avatarSeed),
                                CircleShape
                            ),
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    val reputationVal = reputations[comment.author] ?: 0
                    val col = getReputationColor(reputationVal)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${getReputationText(reputationVal).take(8)}..)",
                        color = col,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "· ${formatTimeAgo(comment.timestamp)}",
                        color = SoftText,
                        fontSize = 11.sp
                    )
                }

                if (comment.isSolution) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SolarCoral.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SOLVED WORKAROUND",
                            color = SolarCoral,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = comment.content,
                color = HeaderText,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            
            // Interaction Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Score
                Row(
                    Modifier.background(CosmicBlack, RoundedCornerShape(12.dp)).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onUpvoteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, null, tint = SoftText, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "${comment.upvotesCount - comment.downvotesCount}",
                        color = HeaderText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDownvoteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = SoftText, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))
                
                // Reply
                Row(
                    modifier = Modifier.clickable { onReplyClick() }.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Share, null, tint = highlightColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reply", color = highlightColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Dialog to Publish a New Post (Opinion or Problem)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostDialog(
    selectedImageUri: android.net.Uri?,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onDismiss: () -> Unit,
    onPublish: (String, String, String, String, String, String?) -> Unit,
    onSaveDraft: ((String, String, String, String) -> Unit)? = null
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var postType by remember { mutableStateOf("OPINION") } // "OPINION" or "PROBLEM"
    var category by remember { mutableStateOf("Tech") }
    var tags by remember { mutableStateOf("") }

    val categories = listOf("Tech", "Work", "Climate", "Society", "Relationships", "Philosophy")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("create_post_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicDark),
            border = BorderStroke(1.dp, CosmicGray)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Share New Thread",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SoftText)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Post Type selector tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { postType = "OPINION" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (postType == "OPINION") NeoCyan else CosmicInput,
                            contentColor = if (postType == "OPINION") CosmicBlack else HeaderText
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("type_opinion_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Opinion (Openia discussion spaces)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { postType = "PROBLEM" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (postType == "PROBLEM") SolarCoral else CosmicInput,
                            contentColor = if (postType == "PROBLEM") CosmicBlack else HeaderText
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("type_problem_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Problem Resolution", fontWeight = FontWeight.Bold)
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = SoftText) },
                    placeholder = { Text("Summarize your thread in one line...", color = SoftText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (postType == "OPINION") NeoCyan else SolarCoral,
                        unfocusedBorderColor = CosmicGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Context/Detail Input
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Thread Details", color = SoftText) },
                    placeholder = { Text("Formulate your core argument or write details of the difficulty experienced...", color = SoftText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("create_content_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (postType == "OPINION") NeoCyan else SolarCoral,
                        unfocusedBorderColor = CosmicGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selection dropdown title
                Text(
                    text = "Select Core Category",
                    color = SoftText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Simulating a simple, beautiful grid selector for Category (more accessible than standard dropdown)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        val selectColor = if (postType == "OPINION") NeoCyan else SolarCoral
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) selectColor else CosmicInput)
                                .clickable { category = cat }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) CosmicBlack else HeaderText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tags Input
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)", color = SoftText) },
                    placeholder = { Text("e.g. ArtificialIntelligence, burnout, environment", color = SoftText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_tags_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (postType == "OPINION") NeoCyan else SolarCoral,
                        unfocusedBorderColor = CosmicGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Attach Photo Section
                if (selectedImageUri != null) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        coil.compose.AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        IconButton(
                            onClick = onClearImage,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(CosmicBlack.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = Color.White)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onPickImage,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftText),
                        border = BorderStroke(1.dp, SoftText)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Attach Photo")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (onSaveDraft != null) {
                        OutlinedButton(
                            onClick = {
                                if (title.trim().isNotEmpty() || content.trim().isNotEmpty()) {
                                    onSaveDraft(title, content, postType, category)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, SoftBorder)
                        ) {
                            Text("Save Draft", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (title.trim().isNotEmpty() && content.trim().isNotEmpty()) {
                                onPublish(title, content, postType, category, tags, selectedImageUri?.toString())
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("publish_thread_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (postType == "OPINION") NeoCyan else SolarCoral,
                            contentColor = CosmicBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = title.trim().isNotEmpty() && content.trim().isNotEmpty()
                    ) {
                        Text(
                            text = "PUBLISH THREAD",
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

// Generates an elegant custom profile avatar brush based on a letter seed
fun generateAvatarBrush(seed: String): Brush {
    val hash = seed.hashCode()
    val startColor = when (hash % 5) {
        0 -> Color(0xFF7C4DFF) // Purple
        1 -> Color(0xFF00E5FF) // Cyan
        2 -> Color(0xFFFF5722) // Coral
        3 -> Color(0xFFFFEB3B) // Accent Yellow
        else -> Color(0xFF00E676) // Electric Green
    }
    val endColor = when ((hash + 1) % 5) {
        0 -> Color(0xFFFF1744) // Vibrant Red
        1 -> Color(0xFFD500F9) // Lavender Purple
        2 -> Color(0xFF2979FF) // Electric Blue
        3 -> Color(0xFF00B0FF) // Light Blue
        else -> Color(0xFFFF9100) // Solar Amber
    }
    return Brush.linearGradient(listOf(startColor, endColor))
}

// Human readable times for timelines
fun formatTimeAgo(timeMs: Long): String {
    val diff = System.currentTimeMillis() - timeMs
    return when {
        diff < 60000 -> "just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}

fun formatFullDate(timeMs: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
    return sdf.format(Date(timeMs))
}

@Composable
fun OldProfileTabContent(
    viewModel: PostViewModel,
    myProfile: com.example.data.model.UserProfileEntity?,
    allFollows: List<String>,
    reputations: Map<String, Int>,
    advancedReputations: Map<String, com.example.domain.model.AdvancedReputation> = emptyMap(),
    posts: List<com.example.data.model.PostEntity> = emptyList()
) {
    var isEditing by remember { mutableStateOf(false) }
    var editDisplayName by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editAvatarSeed by remember { mutableStateOf("") }
    var profileTab by remember { mutableStateOf(0) } // 0: Posts, 1: Saved, 2: Topics

    val myPosts = remember(posts) {
        posts.filter { it.author == "You" }.sortedByDescending { it.timestamp }
    }
    val savedPosts = remember(posts) {
        // Just mock some saved posts based on upvotes for now or empty
        posts.filter { it.upvotesCount > 5 }.take(3)
    }

    LaunchedEffect(myProfile) {
        myProfile?.let {
            editDisplayName = it.displayName
            editBio = it.bio
            editAvatarSeed = it.avatarSeed
        }
    }

    val myPoints = reputations["You"] ?: 0
    val myBadge = getReputationText(myPoints)
    val myColor = getReputationColor(myPoints)
    val totalFollowers = (myProfile?.baseFollowers ?: 12) + maxOf(0, myPoints * 2)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Modern Profile Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                // Banner Background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(AccentPurple.copy(alpha = 0.8f), NeoCyan.copy(alpha = 0.8f))
                            )
                        )
                )

                // Avatar and Profile Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 90.dp)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(CosmicBlack, CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    generateAvatarBrush(if (isEditing) editAvatarSeed else (myProfile?.avatarSeed ?: "Y")),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isEditing) editAvatarSeed else (myProfile?.avatarSeed ?: "Y"),
                                color = Color.White,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isEditing) {
                        Text(
                            text = myProfile?.displayName ?: "Openian Citizen",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.testTag("profile_display_name")
                        )
                        Text(
                            text = "@You (Anonymous Identity)",
                            color = SoftText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = myProfile?.bio ?: "Sharing alternative insights and solutions on Openia.",
                            color = HeaderText,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.testTag("profile_bio").padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { isEditing = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicDark, contentColor = NeoCyan),
                            border = BorderStroke(1.dp, NeoCyan.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(40.dp).testTag("edit_profile_btn"),
                            contentPadding = PaddingValues(horizontal = 24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Profile Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        if (isEditing) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicGray)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Edit Profile",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = editDisplayName,
                            onValueChange = { editDisplayName = it },
                            label = { Text("Display Name", color = SoftText) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeoCyan,
                                unfocusedBorderColor = CosmicDark,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("edit_display_name_field")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { editBio = it },
                            label = { Text("Profile Bio", color = SoftText) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeoCyan,
                                unfocusedBorderColor = CosmicDark,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("edit_bio_field")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Choose Your Avatar Profile Seed (Letter)",
                            color = SoftText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val availableSeeds = ('A'..'Z').toList().map { it.toString() }
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(availableSeeds) { letter ->
                                val isSelected = editAvatarSeed == letter
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(generateAvatarBrush(letter))
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { editAvatarSeed = letter }
                                        .testTag("seed_choice_$letter"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = letter,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { isEditing = false },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicDark, contentColor = SoftText),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("cancel_edit_btn")
                            ) {
                                Text("Cancel", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    if (editDisplayName.trim().isNotEmpty()) {
                                        viewModel.updateMyProfile(editDisplayName, editBio, editAvatarSeed)
                                        isEditing = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("save_profile_btn"),
                                enabled = editDisplayName.trim().isNotEmpty()
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Summary Statistics Section
        if (!isEditing) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Gamification Metric Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CosmicGray)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "$totalFollowers", color = NeoCyan, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Followers", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CosmicGray)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "${allFollows.size}", color = AccentPurple, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Following", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CosmicGray)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "$myPoints", color = SolarCoral, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Points", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 2. Game-like Reputation Progression Section
        val userAdvancedReputation = advancedReputations["You"]
        if (userAdvancedReputation != null) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AdvancedProfileCard(
                        username = "You",
                        displayName = myProfile?.displayName ?: "Openian Citizen",
                        reputation = userAdvancedReputation
                    )
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("reputation_status_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicGray)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Your Likability Reputation",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Current Level Badge",
                                    color = SoftText,
                                    fontSize = 11.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(myColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = myBadge,
                                    color = myColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Dynamic progress calculations
                        val rangeInfo = when {
                            myPoints < 10 -> Triple(0, 10, "Credible Peer")
                            myPoints in 10..30 -> Triple(10, 31, "Insightful Mentor")
                            myPoints in 31..70 -> Triple(31, 71, "Consensus Analyst")
                            myPoints in 71..150 -> Triple(71, 151, "Legendary Citizen")
                            else -> Triple(151, 151, "Maximum Authority achieved!")
                        }
                        val (lowerBound, upperBound, nextLevelName) = rangeInfo
                        val progress = if (upperBound == lowerBound) 1.0f else {
                            (myPoints - lowerBound).toFloat() / (upperBound - lowerBound).toFloat()
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (upperBound == lowerBound) "Max Level Achieved!" else "Next Level: $nextLevelName",
                                color = SoftText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (upperBound == lowerBound) "Top Tier" else "${myPoints - lowerBound} / ${upperBound - lowerBound}",
                                color = SoftText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0.0f, 1.0f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            color = myColor,
                            trackColor = CosmicInput
                        )
                    }
                }
            }
        }

        // 3. Followed Authors Trackers
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("following_list_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicGray)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Following (${allFollows.size})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (allFollows.isEmpty()) {
                        Text(
                            text = "You aren't following reference thinkers yet. Tap Follow next to author names list like @NavalS to monitor their feeds easily or filter the dashboard content!",
                            color = SoftText,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    } else {
                        // Chips layout using LazyRow
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(allFollows) { author ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CosmicDark)
                                        .border(1.dp, CosmicInput, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                        .testTag("followed_chip_$author")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    generateAvatarBrush(author.firstOrNull()?.toString() ?: "A"),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = author.firstOrNull()?.toString() ?: "A",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "@$author",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Unfollow Author",
                                            tint = SolarCoral,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { viewModel.toggleFollow(author) }
                                                .testTag("unfollow_chip_btn_$author")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicDark),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val tabs = listOf("My Posts", "Saved Posts")
                tabs.forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { profileTab = index }
                            .then(
                                if (profileTab == index) Modifier.background(NeoCyan.copy(alpha = 0.2f))
                                else Modifier
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (profileTab == index) NeoCyan else SoftText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        val displayedPosts = if (profileTab == 0) myPosts else savedPosts

        if (displayedPosts.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No posts found here.", color = SoftText)
                }
            }
        } else {
            items(displayedPosts, key = { "profile_post_${it.id}" }) { post ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PostCard(
                        post = post,
                        followedAuthors = allFollows,
                        reputations = reputations,
                        advancedReputations = advancedReputations,
                        onFollowToggle = null,
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

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
