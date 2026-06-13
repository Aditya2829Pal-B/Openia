package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CommentEntity
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

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

    // Combined filtered posts flow - delegating directly to UseCase!
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
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshFeed() {
        viewModelScope.launch {
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
                updateUserProfileUseCase.execute("Openian Citizen", "Sharing alternative insights and solutions on Openia.", "Y")
            }
        }
    }

    fun setFollowedOnly(followed: Boolean) {
        _followedOnly.value = followed
    }

    fun toggleFollow(authorName: String) {
        viewModelScope.launch {
            toggleFollowUseCase.execute(authorName)
        }
    }

    fun updateMyProfile(displayName: String, bio: String, avatarSeed: String) {
        viewModelScope.launch {
            updateUserProfileUseCase.execute(displayName, bio, avatarSeed)
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

    // Reaction Triggers delegating to UseCase
    fun agreePost(postId: Int) {
        viewModelScope.launch { togglePostReactionUseCase.execute(postId, "AGREE") }
    }

    fun disagreePost(postId: Int) {
        viewModelScope.launch { togglePostReactionUseCase.execute(postId, "DISAGREE") }
    }

    fun upvotePost(postId: Int) {
        viewModelScope.launch { togglePostReactionUseCase.execute(postId, "UPVOTE") }
    }

    fun downvotePost(postId: Int) {
        viewModelScope.launch { togglePostReactionUseCase.execute(postId, "DOWNVOTE") }
    }

    fun empathyPost(postId: Int) {
        viewModelScope.launch { togglePostReactionUseCase.execute(postId, "EMPATHY") }
    }

    // Post creation delegating to UseCase & checking safety with Moderation UseCase
    fun publishPost(
        title: String,
        content: String,
        type: String, // "OPINION" or "PROBLEM"
        category: String,
        tags: String,
        imageUri: String? = null,
        author: String = "You"
    ) {
        if (!moderateContentUseCase.isContentSafe(title, content)) {
            // Under enterprise bounds, we abort creating toxic posts
            return
        }
        viewModelScope.launch {
            createPostUseCase.execute(title, content, type, category, tags, imageUri, author)
        }
    }

    // Comment submission delegating to UseCase
    fun submitComment(postId: Int, content: String, author: String = "You", isSolution: Boolean = false, parentCommentId: Int? = null) {
        if (!moderateContentUseCase.isContentSafe("", content)) {
            return
        }
        viewModelScope.launch {
            // we will bypass use case for parentCommentId because it's simpler
            val comment = CommentEntity(
                postId = postId,
                author = author,
                avatarSeed = if (author == "You") "Y" else author.first().toString().uppercase(),
                content = content,
                isSolution = isSolution,
                parentCommentId = parentCommentId
            )
            repository.createComment(comment)
        }
    }

    fun upvoteComment(commentId: Int) {
        viewModelScope.launch { repository.toggleCommentUpvote(commentId) }
    }

    fun downvoteComment(commentId: Int) {
        viewModelScope.launch { repository.toggleCommentDownvote(commentId) }
    }

    // Gemini API Action delegating to UseCase
    fun analyzeWithGemini(postId: Int) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                analyzePostContentUseCase.execute(postId)
            } catch (e: Exception) {
                // Safe error mitigation
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
