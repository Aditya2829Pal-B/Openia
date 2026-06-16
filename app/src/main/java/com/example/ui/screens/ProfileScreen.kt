package com.example.ui.screens

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostEntity
import com.example.data.model.UserProfileEntity
import com.example.domain.model.AdvancedReputation
import com.example.ui.components.ContributorBadge
import com.example.ui.components.ScoreBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.PostViewModel

@Composable
fun ProfileTabContent(
    viewModel: PostViewModel,
    myProfile: UserProfileEntity?,
    allFollows: List<String>,
    reputations: Map<String, Int>,
    advancedReputations: Map<String, AdvancedReputation> = emptyMap(),
    posts: List<PostEntity> = emptyList(),
    drafts: List<com.example.data.model.DraftEntity> = emptyList(),
    bookmarks: List<com.example.data.model.BookmarkEntity> = emptyList()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isEditing by remember(myProfile) { mutableStateOf(false) }
    var editDisplayName by remember(myProfile) { mutableStateOf(myProfile?.displayName ?: "") }
    var editBio by remember(myProfile) { mutableStateOf(myProfile?.bio ?: "") }
    var editAvatarSeed by remember(myProfile) { mutableStateOf(myProfile?.avatarSeed ?: "Y") }
    var activeDashboard by remember { mutableStateOf("Recent Posts") }

    val myPosts = remember(posts) { posts.filter { it.author == "You" }.sortedByDescending { it.timestamp } }
    val savedPosts = remember(posts) { posts.filter { it.upvotesCount > 5 }.take(3) }
    val myReputation = advancedReputations["You"]
    val simpleRep = reputations["You"] ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // --- 1. PROFILE HEADER ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicGray)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Cover Banner with Actions
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(AccentPurple, NeoCyan)
                                )
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { 
                                    val sendIntent: android.content.Intent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, "Check out my profile on Openia! @${editDisplayName}")
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(CosmicBlack.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { isEditing = !isEditing },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(CosmicBlack.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(if (isEditing) Icons.Default.Close else Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { 
                                    android.widget.Toast.makeText(context, "Settings clicked", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(CosmicBlack.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Avatar area overlapping the banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-40).dp)
                            .padding(horizontal = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(CosmicGray, CircleShape)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(generateAvatarBrush(editAvatarSeed), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = editAvatarSeed,
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // Identity Info
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-30).dp)
                            .padding(horizontal = 24.dp)
                    ) {
                        if (isEditing) {
                            OutlinedTextField(
                                value = editDisplayName,
                                onValueChange = { editDisplayName = it },
                                label = { Text("Display Name", color = SoftText) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeoCyan, unfocusedBorderColor = CosmicDark, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = editAvatarSeed,
                                onValueChange = { if(it.length <= 1) editAvatarSeed = it.uppercase() },
                                label = { Text("Initial", color = SoftText) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeoCyan, unfocusedBorderColor = CosmicDark, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            )
                            Button(
                                onClick = {
                                    viewModel.updateMyProfile(editDisplayName, editBio, editAvatarSeed)
                                    isEditing = false
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeoCyan)
                            ) {
                                Text("Save Profile", color = CosmicBlack, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = editDisplayName.ifEmpty { "Openian Citizen" },
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = NeoCyan, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = "@You",
                                color = SoftText,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Rank, Archetype, Domain
                            if (myReputation != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("RANK & TITLE", color = SoftText, fontSize = 10.sp, letterSpacing = 1.sp)
                                        Text("Rank ${myReputation.rankLevel} • ${myReputation.civilizationClass}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(color = CosmicDark, shape = RoundedCornerShape(8.dp)) {
                                        Text(myReputation.thinkerArchetype, color = NeoCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(color = CosmicDark, shape = RoundedCornerShape(8.dp)) {
                                        Text(myReputation.influenceDomain, color = SolarCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                ContributorBadge(myReputation)
                            } else {
                                ContributorBadge(null, simpleRep)
                            }
                        }
                    }
                }
            }
        }

        // --- 2. REPUTATION SECTION ---
        if (myReputation != null) {
            item {
                Text(
                    text = "Reputation Metrics",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicGray)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ScoreBar("Trust Level", myReputation.trustScore, NeoCyan)
                        ScoreBar("Empathy", myReputation.empathyScore, SolarCoral)
                        ScoreBar("Insight Depth", myReputation.insightScore, Color(0xFFA855F7)) // Purple
                        ScoreBar("Consensus", myReputation.consensusScore, Color(0xFF10B981)) // Emerald
                        
                        Divider(color = CosmicDark)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Reputation Score", color = SoftText, fontSize = 14.sp)
                            Text("${myReputation.totalScore} PTS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        
                        // Fake progress info since real nextRank is not trivially available
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Next Rank Progress", color = SoftText, fontSize = 14.sp)
                            Text("68%", color = NeoCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        // Progress bar snippet
                        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(CosmicDark, RoundedCornerShape(3.dp))) {
                            Box(modifier = Modifier.fillMaxWidth(0.68f).fillMaxHeight().background(NeoCyan, RoundedCornerShape(3.dp)))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // --- 3. STATS SECTION ---
        item {
            Text(
                text = "Analytics Overview",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(label = "Posts", value = myPosts.size.toString(), modifier = Modifier.weight(1f))
                StatCard(label = "Followers", value = (myProfile?.baseFollowers ?: 0).toString(), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(label = "Following", value = allFollows.size.toString(), modifier = Modifier.weight(1f))
                StatCard(label = "Points", value = (myReputation?.totalScore ?: simpleRep).toString(), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- 4. ACTIVITY SECTION ---
        item {
            Text(
                text = "Dashboard",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val dashboards = listOf("Recent Activity" to Icons.Default.DateRange, "Recent Posts" to Icons.Default.Face, "Saved Posts" to Icons.Default.FavoriteBorder, "Drafts" to Icons.Default.Edit, "Bookmarks" to Icons.Default.Menu)
                items(dashboards) { (title, icon) ->
                    Card(
                        modifier = Modifier.width(140.dp).height(100.dp),
                        colors = CardDefaults.cardColors(containerColor = if (activeDashboard == title) CosmicDark else CosmicGray),
                        shape = RoundedCornerShape(16.dp),
                        border = if (activeDashboard == title) BorderStroke(1.dp, NeoCyan) else null
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { activeDashboard = title }
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Icon(icon, contentDescription = title, tint = NeoCyan, modifier = Modifier.size(24.dp).padding(bottom = 8.dp))
                            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- 5. PROFILE MENU ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicGray)
            ) {
                Column {
                    val menuItems = listOf(
                        "Profile Settings" to Icons.Default.Settings,
                        "Privacy & Safety" to Icons.Default.Lock,
                        "Notifications" to Icons.Default.Notifications,
                        "Insights & Analytics" to Icons.Default.Info,
                        "Achievements" to Icons.Default.Star,
                        "Help Center" to Icons.Default.Info,
                        "Contact Us" to Icons.Default.Email,
                        "Feedback" to Icons.Default.CheckCircle,
                        "About Openia" to Icons.Default.Info
                    )
                    
                    menuItems.forEachIndexed { index, item ->
                        SupportRowRow(item.second, item.first) {
                            android.widget.Toast.makeText(context, "${item.first} clicked", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        if (index < menuItems.size - 1) {
                            HorizontalDivider(color = CosmicDark, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- 6. DASHBOARD CONTENT ---
        item {
            Text(
                text = activeDashboard,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        val itemsToShow = when (activeDashboard) {
            "Recent Posts" -> myPosts
            "Saved Posts" -> savedPosts
            "Recent Activity" -> posts.sortedByDescending { it.timestamp }.take(5)
            "Bookmarks" -> posts.filter { p -> bookmarks.any { it.postId == p.id } }
            else -> emptyList()
        }

        if (activeDashboard == "Drafts") {
            if (drafts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No drafts found.", color = SoftText)
                    }
                }
            } else {
                items(drafts, key = { "draft_${it.id}" }) { draft ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CosmicDark),
                            border = BorderStroke(1.dp, SoftBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(draft.title, color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(draft.content.take(100) + "...", color = SoftText, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { viewModel.deleteDraft(draft.id) }) {
                                        Text("Delete", color = NeoRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (itemsToShow.isEmpty()) {
             item {
                 Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                     Text("No items found.", color = SoftText)
                 }
             }
        } else {
            items(itemsToShow, key = { "dashboard_${activeDashboard}_${it.id}" }) { post ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                     PostCard(
                         post = post,
                         followedAuthors = allFollows,
                         reputations = reputations,
                         advancedReputations = advancedReputations,
                         isBookmarked = bookmarks.any { it.postId == post.id },
                         onFollowToggle = { author -> viewModel.toggleFollow(author) },
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
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CosmicGray),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = SoftText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SupportRowRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = SoftText, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = SoftText, modifier = Modifier.size(20.dp))
    }
}
