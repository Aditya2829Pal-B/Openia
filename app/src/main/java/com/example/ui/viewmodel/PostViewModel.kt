package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CommentEntity
import com.example.data.model.Discussion
import com.example.data.model.PostEntity
import com.example.data.repository.PostRepository
import com.example.domain.usecase.ai.AnalyzePostContentUseCase
import com.example.domain.usecase.analytics.GetTrendMetricsUseCase
import com.example.domain.usecase.auth.AuthenticateUserUseCase
import com.example.domain.usecase.feed.*
import com.example.domain.usecase.followers.ToggleFollowUseCase
import com.example.domain.usecase.moderation.ModerateContentUseCase
import com.example.domain.usecase.notifications.ManageNotificationsUseCase
import com.example.domain.usecase.profile.GetUserProfileUseCase
import com.example.domain.usecase.profile.UpdateUserProfileUseCase
import com.example.domain.usecase.reputation.CalculateUserReputationUseCase
import com.example.domain.usecase.settings.ManageSettingsUseCase
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.plus
import kotlinx.coroutines.CoroutineExceptionHandler
import com.example.core.error.GlobalErrorHandler

@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModel(
    application: Application,
    private val repository: PostRepository,
    private val getFeedPostsUseCase: GetFeedPostsUseCase,
    private val createPostUseCase: CreatePostUseCase,
    private val togglePostReactionUseCase: TogglePostReactionUseCase,
    private val addPostCommentUseCase: AddPostCommentUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val toggleFollowUseCase: ToggleFollowUseCase,
    private val calculateUserReputationUseCase: CalculateUserReputationUseCase,
    private val analyzePostContentUseCase: AnalyzePostContentUseCase,
    private val manageNotificationsUseCase: ManageNotificationsUseCase,
    private val manageSettingsUseCase: ManageSettingsUseCase,
    private val moderateContentUseCase: ModerateContentUseCase,
    private val getTrendMetricsUseCase: GetTrendMetricsUseCase,
    private val authenticateUserUseCase: AuthenticateUserUseCase
) : AndroidViewModel(application) {

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        GlobalErrorHandler.handleThrowable(exception)
    }

    private val safeScope = viewModelScope.plus(exceptionHandler)

    // Filter states
    private val _selectedType = MutableStateFlow("ALL") // "ALL", "OPINION", "PROBLEM"
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    private val _selectedCategory = MutableStateFlow("ALL")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSort = MutableStateFlow("TRENDING") // "LATEST", "TRENDING"
    val selectedSort: StateFlow<String> = _selectedSort.asStateFlow()

    // Follower and Profile State
    private val _followedOnly = MutableStateFlow(false)
    val followedOnly: StateFlow<Boolean> = _followedOnly.asStateFlow()

    val allFollows: StateFlow<List<String>> = repository.allFollows
        .map { list -> list.map { it.followedAuthor } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Combined filtered posts flow natively with InvalidatingPagingSourceFactory
    private val pagingSourceFactory = androidx.paging.InvalidatingPagingSourceFactory {
        repository.getDynamicPagingSource(
            _selectedType.value,
            _selectedCategory.value,
            _searchQuery.value,
            _followedOnly.value,
            allFollows.value
        )
    }

    val allDiscussions: StateFlow<List<Discussion>> = repository.allDiscussions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pagedPosts: kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<PostEntity>> = androidx.paging.Pager(
        config = androidx.paging.PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = pagingSourceFactory
    ).flow.cachedIn(viewModelScope)

    init {
        // Invalidate Pager when filters change
        viewModelScope.launch {
            combine(
                _selectedType,
                _selectedCategory,
                _searchQuery,
                _followedOnly,
                allFollows
            ) { _, _, _, _, _ -> 1 }
                .collect {
                    pagingSourceFactory.invalidate()
                }
        }
    }

    val posts: StateFlow<List<PostEntity>> = getFeedPostsUseCase.execute(
        selectedType = _selectedType,
        selectedCategory = _selectedCategory,
        searchQuery = _searchQuery,
        selectedSort = _selectedSort,
        followedOnly = _followedOnly,
        allFollows = allFollows
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Single post detail monitoring state
    private val _currentPostId = MutableStateFlow<Int?>(null)

    // Single discussion detail monitoring state
    private val _selectedDiscussionId = MutableStateFlow<Int?>(null)
    val selectedDiscussionId: StateFlow<Int?> = _selectedDiscussionId.asStateFlow()

    val currentDiscussion: StateFlow<Discussion?> = _selectedDiscussionId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getDiscussionById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentDiscussionComments: StateFlow<List<CommentEntity>> = _selectedDiscussionId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getCommentsForPost(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshFeed() {
        safeScope.launch {
            _isRefreshing.value = true
            // Simulate network fetch and local database sync
            kotlinx.coroutines.delay(1000)
            _isRefreshing.value = false
        }
    }

    val currentPost: StateFlow<PostEntity?> = _currentPostId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getPostById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentComments: StateFlow<List<CommentEntity>> = _currentPostId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getCommentsForPost(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentReactions: StateFlow<List<String>> = _currentPostId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getUserReactionsForPost(id).map { list -> list.map { it.reactionType } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Loading states for AI analysis
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // Exposed UserProfile from UseCase
    val myProfile: StateFlow<com.example.data.model.UserProfileEntity?> = getUserProfileUseCase.execute("You")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val drafts: StateFlow<List<com.example.data.model.DraftEntity>> = repository.allDrafts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bookmarks: StateFlow<List<com.example.data.model.BookmarkEntity>> = repository.allBookmarks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reputation Map calculated elegantly through UseCase
    val authorReputations: StateFlow<Map<String, Int>> = calculateUserReputationUseCase.execute()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
        
    val advancedReputations: StateFlow<Map<String, com.example.domain.model.AdvancedReputation>> = calculateUserReputationUseCase.executeDetailed()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    init {
        // Prepopulate with a baseline of initial high quality posts and default profile if database is empty
        viewModelScope.launch {
            repository.allPosts.first().let { currentList ->
                if (currentList.isEmpty()) {
                    createDefaultBaseline()
                }
            }
            // Ensure default profile exists
            val existingProfile = getUserProfileUseCase.executeDirect("You")
            if (existingProfile == null) {
                updateUserProfileUseCase.execute("You", "You", "Openian Citizen", "Sharing alternative insights and solutions on Openia.", "Y", null)
            }
        }
    }

    fun setFollowedOnly(followed: Boolean) {
        _followedOnly.value = followed
    }

    fun toggleFollow(authorName: String) {
        safeScope.launch {
            toggleFollowUseCase.execute(authorName)
        }
    }

    fun updateMyProfile(oldUsername: String, newUsername: String, displayName: String, bio: String, avatarSeed: String, profilePictureUri: String?) {
        viewModelScope.launch {
            updateUserProfileUseCase.execute(oldUsername, newUsername, displayName, bio, avatarSeed, profilePictureUri)
        }
    }

    fun setFilterType(type: String) {
        _selectedType.value = type
    }

    fun setFilterCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(sort: String) {
        _selectedSort.value = sort
    }

    fun selectPost(postId: Int?) {
        _currentPostId.value = postId
    }

    fun selectDiscussion(discussionId: Int?) {
        _selectedDiscussionId.value = discussionId
    }

    fun upvoteDiscussion(discussionId: Int) {
        safeScope.launch { repository.upvoteDiscussion(discussionId) }
    }

    fun downvoteDiscussion(discussionId: Int) {
        safeScope.launch { repository.downvoteDiscussion(discussionId) }
    }

    fun submitDiscussionComment(
        discussionId: Int,
        content: String,
        author: String? = null,
        isSolution: Boolean = false,
        parentCommentId: Int? = null
    ) {
        if (!moderateContentUseCase.isContentSafe("", content)) {
            return
        }
        safeScope.launch {
            val currentUsername = myProfile.value?.displayName ?: myProfile.value?.username ?: "You"
            val finalAuthor = author ?: currentUsername
            val comment = CommentEntity(
                postId = discussionId,
                author = finalAuthor,
                avatarSeed = if (finalAuthor == "You") "Y" else finalAuthor.first().toString().uppercase(),
                content = content,
                isSolution = isSolution,
                parentCommentId = parentCommentId
            )
            repository.createComment(comment)
            if (isSolution) {
                repository.awardReputationPoint(
                    userId = 1,
                    points = 25,
                    source = "ACTIONABLE_SOLUTION",
                    description = "Contributed an actionable solution to problem #$discussionId"
                )
            }
        }
    }

    fun contributeActionableSolution(
        discussionId: Int,
        title: String,
        steps: String,
        outcome: String,
        parentCommentId: Int? = null
    ) {
        val formatted = buildString {
            append("💡 Actionable Resolution: ").append(title.trim()).append("\n\n")
            if (steps.isNotBlank()) {
                append("📌 Implementation Steps:\n").append(steps.trim()).append("\n\n")
            }
            if (outcome.isNotBlank()) {
                append("🎯 Expected Outcome & Impact:\n").append(outcome.trim())
            }
        }
        submitDiscussionComment(
            discussionId = discussionId,
            content = formatted,
            isSolution = true,
            parentCommentId = parentCommentId
        )
    }

    // Reaction Triggers delegating to UseCase
    fun agreePost(postId: Int) {
        safeScope.launch { togglePostReactionUseCase.execute(postId, "AGREE") }
    }

    fun disagreePost(postId: Int) {
        safeScope.launch { togglePostReactionUseCase.execute(postId, "DISAGREE") }
    }

    fun upvotePost(postId: Int) {
        safeScope.launch { togglePostReactionUseCase.execute(postId, "UPVOTE") }
    }

    fun downvotePost(postId: Int) {
        safeScope.launch { togglePostReactionUseCase.execute(postId, "DOWNVOTE") }
    }

    fun empathyPost(postId: Int) {
        safeScope.launch { togglePostReactionUseCase.execute(postId, "EMPATHY") }
    }

    // Post creation delegating to UseCase & checking safety with Moderation UseCase
    fun publishPost(
        title: String,
        content: String,
        type: String, // "OPINION" or "PROBLEM"
        category: String,
        tags: String,
        imageUri: String? = null,
        author: String? = null
    ) {
        if (!moderateContentUseCase.isContentSafe(title, content)) {
            // Under enterprise bounds, we abort creating toxic posts
            return
        }
        safeScope.launch {
            val currentUsername = myProfile.value?.username ?: "You"
            val finalAuthor = author ?: currentUsername
            createPostUseCase.execute(title, content, type, category, tags, imageUri, finalAuthor)
        }
    }

    // Comment submission delegating to UseCase
    fun submitComment(postId: Int, content: String, author: String? = null, isSolution: Boolean = false, parentCommentId: Int? = null) {
        if (!moderateContentUseCase.isContentSafe("", content)) {
            return
        }
        safeScope.launch {
            val currentUsername = myProfile.value?.username ?: "You"
            val finalAuthor = author ?: currentUsername
            // we will bypass use case for parentCommentId because it's simpler
            val comment = CommentEntity(
                postId = postId,
                author = finalAuthor,
                avatarSeed = if (finalAuthor == "You") "Y" else finalAuthor.first().toString().uppercase(),
                content = content,
                isSolution = isSolution,
                parentCommentId = parentCommentId
            )
            repository.createComment(comment)
        }
    }

    fun upvoteComment(commentId: Int) {
        safeScope.launch { repository.toggleCommentUpvote(commentId) }
    }

    fun downvoteComment(commentId: Int) {
        safeScope.launch { repository.toggleCommentDownvote(commentId) }
    }

    // Gemini API Action delegating to UseCase
    fun analyzeWithGemini(postId: Int) {
        safeScope.launch {
            _isAnalyzing.value = true
            try {
                analyzePostContentUseCase.execute(postId)
            } catch (e: Exception) {
                GlobalErrorHandler.handleThrowable(e, retryAction = { analyzeWithGemini(postId) })
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    private suspend fun createDefaultBaseline() {
        val data = listOf(
            PostEntity(
                postType = "OPINION",
                title = "Prompt engineering is temporary; natural guidance will rule.",
                content = "Within 2 years, the notion of 'crafting a prompt' with specific tags and delimiters will feel archaic. LLMs will understand contextual conversations so perfectly that talking to an AI will feel exactly like instructing a highly capable colleague. Focus on systems thinking, not prompt templates.",
                category = "Tech",
                tags = "AI,Future,TechOpinions",
                author = "NavalS",
                avatarSeed = "N",
                agreeCount = 28,
                disagreeCount = 6,
                upvotesCount = 35,
                timestamp = System.currentTimeMillis() - 7200000
            ),
            PostEntity(
                postType = "PROBLEM",
                title = "Burnout is seen as a badge of honor in my startup.",
                content = "People literally boast about commits made at 3:00 AM on Sundays. I feel complete anxiety when I sign off at 6:00 PM on weekdays, even though my work is done. It's destroying my mental health, but speaking up would mark me as 'unmotivated'. How do I challenge this toxic dynamic quietly?",
                category = "Work",
                tags = "MentalHealth,StartupCulture,Burnout",
                author = "AnxiousCoder",
                avatarSeed = "A",
                upvotesCount = 42,
                empathyCount = 29,
                timestamp = System.currentTimeMillis() - 14400000
            ),
            PostEntity(
                postType = "OPINION",
                title = "We must pass laws to strictly regulate microplastics now.",
                content = "Every week we read a new study confirming microplastics have breached the blood-brain barrier, found in placenta, and rainfall. Trying to 'avoid' plastic as an individual is a neat marketing illusion designed to deflect system responsibility. The only response is immediate global regulatory bans at the chemical manufacture level.",
                category = "Climate",
                tags = "Environment,Health,Policy",
                author = "EcoWarrior",
                avatarSeed = "E",
                agreeCount = 56,
                disagreeCount = 2,
                upvotesCount = 62,
                timestamp = System.currentTimeMillis() - 25200000
            ),
            PostEntity(
                postType = "PROBLEM",
                title = "Self-hosting personal data is still too hard for regular folks.",
                content = "Google is scanning all files, subscription prices are soaring, and I want out. But self-hosting a Synology or Nextcloud takes 30 hours of setup, domain configuration, port-forwarding, and constant anxiety over hard drive failures. Why hasn't a truly plug-and-play local cloud box for normal families succeeded yet?",
                category = "Tech",
                tags = "SelfHosting,Privacy,DataSecurity",
                author = "PrivacySceptic",
                avatarSeed = "P",
                upvotesCount = 27,
                empathyCount = 18,
                timestamp = System.currentTimeMillis() - 86400000
            )
        )

        for (post in data) {
            val newId = repository.createPost(post).toInt()
            
            // Add some initial comments to the burnout problem to make it interactive
            if (post.postType == "PROBLEM" && post.title.contains("Startup")) {
                repository.createComment(
                    CommentEntity(
                        postId = newId,
                        author = "WiseSage",
                        avatarSeed = "W",
                        content = "This is a classic 'toxic normalization' trap. Try scheduling your Slack/email messages to deliver at 8:30 AM instead of sending them immediately at 6:00 PM, creating a psychological buffer. Also, quietly format your portfolio and start interviewing elsewhere. Cultures rarely change until key talent leaves.",
                        timestamp = System.currentTimeMillis() - 10000000,
                        isSolution = true,
                        upvotesCount = 12
                    )
                )
                repository.createComment(
                    CommentEntity(
                        postId = newId,
                        author = "DevDan",
                        avatarSeed = "D",
                        content = "I suffered through this for 3 years. Please don't trade your physical and mental youth for standard stock options that might turn out worthless. Speak to a medical therapist today. High stress impairs cognitive function deeply.",
                        timestamp = System.currentTimeMillis() - 5000000,
                        isSolution = false,
                        upvotesCount = 5
                    )
                )
            }
        }

        // Initialize sample users and discussions for DiscussionFeedScreen and DiscussionDetailScreen
        val defaultUsers = listOf(
            com.example.data.model.User(
                id = 1,
                username = "elena_rostova",
                displayName = "Dr. Elena Rostova",
                bio = "Distributed Systems Researcher & Core Architect",
                isVerified = true
            ),
            com.example.data.model.User(
                id = 2,
                username = "marcus_vance",
                displayName = "Marcus Vance",
                bio = "Staff SRE, cloud infrastructure and concurrency",
                isVerified = true
            ),
            com.example.data.model.User(
                id = 3,
                username = "sarah_lin",
                displayName = "Sarah Lin",
                bio = "Community lead & civic tech researcher",
                isVerified = false
            )
        )
        repository.insertUsers(defaultUsers)

        val defaultDiscussions = listOf(
            Discussion(
                id = 101,
                authorId = 1,
                title = "High Latency in Distributed State Synchronization Under High Churn",
                content = "We have been observing substantial p99 latency spikes (exceeding 850ms) across geographically distributed node clusters during bursts of churn. The bottleneck appears during vector clock reconciliation and read-repair phases across regions with asymmetric bandwidth. We need concrete actionable architectural strategies to reduce reconciliation latency without sacrificing monotonic read consistency.",
                category = "Architecture",
                tags = "distributed-systems,concurrency,database,latency",
                upvotes = 42,
                downvotes = 3,
                createdAt = System.currentTimeMillis() - 86400000
            ),
            Discussion(
                id = 102,
                authorId = 2,
                title = "Mitigating Algorithmic Echo Chambers Without Degrading Organic Engagement",
                content = "Current recommender algorithms optimize strictly for immediate engagement loops, which mathematically amplifies hyper-partisan echo chambers and extreme viewpoints. How can social platforms design serendipity heuristics and counter-perspective discovery into the core feed while keeping bounce rates acceptable?",
                category = "Social Ethics",
                tags = "ai-ethics,algorithms,social-media,society",
                upvotes = 67,
                downvotes = 5,
                createdAt = System.currentTimeMillis() - 172800000
            ),
            Discussion(
                id = 103,
                authorId = 3,
                title = "Affordable Microgrid Energy Storage Solutions for Remote Communities",
                content = "Remote and off-grid mountain communities are heavily reliant on diesel generators despite having abundant seasonal hydro and solar potential. The primary challenge is short-duration vs long-duration energy storage degradation during freezing winters. Looking for tested, actionable solutions for modular thermal or flow-battery setups that can be maintained locally without specialized engineers.",
                category = "Clean Tech",
                tags = "energy,microgrids,sustainability,storage",
                upvotes = 35,
                downvotes = 1,
                createdAt = System.currentTimeMillis() - 259200000
            )
        )
        repository.insertDiscussions(defaultDiscussions)

        // Seed nested comments for Discussion #101 to demonstrate recursive structure
        val rootCommentId = 2001
        repository.createComment(
            CommentEntity(
                id = rootCommentId,
                postId = 101,
                author = "Marcus Vance",
                avatarSeed = "M",
                content = "Have you evaluated state-based Conflict-free Replicated Data Types (CvRDTs) with delta-mutations? We eliminated our reconciliation lock bottleneck by transmitting only observed deltas instead of full causal states.",
                timestamp = System.currentTimeMillis() - 72000000,
                isSolution = false,
                upvotesCount = 19
            )
        )

        val childCommentId = 2002
        repository.createComment(
            CommentEntity(
                id = childCommentId,
                postId = 101,
                author = "Dr. Elena Rostova",
                avatarSeed = "E",
                content = "We tested delta-CRDTs in initial benchmarks, but garbage collection of tombstones during network partitions caused memory ballooning. How did you handle tombstone compaction safely?",
                timestamp = System.currentTimeMillis() - 65000000,
                isSolution = false,
                upvotesCount = 14,
                parentCommentId = rootCommentId
            )
        )

        val grandchildCommentId = 2003
        repository.createComment(
            CommentEntity(
                id = grandchildCommentId,
                postId = 101,
                author = "Marcus Vance",
                avatarSeed = "M",
                content = "💡 Actionable Solution: Epoch-based Tombstone Pruning Protocol.\n\n1. Maintain an epoch timestamp watermark acknowledged by quorum.\n2. Purge tombstones older than maximum replication timeout (e.g. 7 days).\n3. Any disconnected partition lagging past the watermark must resync via full snapshot rather than delta catchup.\n\nThis reduced our memory overhead by 82% and dropped p99 to 110ms.",
                timestamp = System.currentTimeMillis() - 50000000,
                isSolution = true,
                upvotesCount = 31,
                parentCommentId = childCommentId
            )
        )

        // Another root comment on Discussion #101
        repository.createComment(
            CommentEntity(
                id = 2004,
                postId = 101,
                author = "Sarah Lin",
                avatarSeed = "S",
                content = "Could client-side predictive optimistic caching relieve the central read-repair pressure?",
                timestamp = System.currentTimeMillis() - 40000000,
                isSolution = false,
                upvotesCount = 8
            )
        )
    }

    fun toggleBookmark(postId: Int) {
        safeScope.launch { repository.toggleBookmark(postId) }
    }
    
    fun saveDraft(title: String, content: String, type: String, category: String) {
        safeScope.launch { 
            repository.saveDraft(com.example.data.model.DraftEntity(
                title = title,
                content = content,
                type = type,
                category = category
            ))
        }
    }
    
    fun deleteDraft(id: Int) {
        safeScope.launch { repository.deleteDraft(id) }
    }
}

class PostViewModelFactory(
    private val application: Application,
    private val repository: PostRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostViewModel::class.java)) {
            val getFeedPostsUseCase = GetFeedPostsUseCase(repository)
            val createPostUseCase = CreatePostUseCase(repository)
            val togglePostReactionUseCase = TogglePostReactionUseCase(repository)
            val addPostCommentUseCase = AddPostCommentUseCase(repository)
            val getUserProfileUseCase = GetUserProfileUseCase(repository)
            val updateUserProfileUseCase = UpdateUserProfileUseCase(repository)
            val toggleFollowUseCase = ToggleFollowUseCase(repository)
            val calculateUserReputationUseCase = CalculateUserReputationUseCase(repository)
            val analyzePostContentUseCase = AnalyzePostContentUseCase(repository)
            val manageNotificationsUseCase = ManageNotificationsUseCase()
            val manageSettingsUseCase = ManageSettingsUseCase()
            val moderateContentUseCase = ModerateContentUseCase()
            val getTrendMetricsUseCase = GetTrendMetricsUseCase()
            val authenticateUserUseCase = AuthenticateUserUseCase(repository)

            @Suppress("UNCHECKED_CAST")
            return PostViewModel(
                application = application,
                repository = repository,
                getFeedPostsUseCase = getFeedPostsUseCase,
                createPostUseCase = createPostUseCase,
                togglePostReactionUseCase = togglePostReactionUseCase,
                addPostCommentUseCase = addPostCommentUseCase,
                getUserProfileUseCase = getUserProfileUseCase,
                updateUserProfileUseCase = updateUserProfileUseCase,
                toggleFollowUseCase = toggleFollowUseCase,
                calculateUserReputationUseCase = calculateUserReputationUseCase,
                analyzePostContentUseCase = analyzePostContentUseCase,
                manageNotificationsUseCase = manageNotificationsUseCase,
                manageSettingsUseCase = manageSettingsUseCase,
                moderateContentUseCase = moderateContentUseCase,
                getTrendMetricsUseCase = getTrendMetricsUseCase,
                authenticateUserUseCase = authenticateUserUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
