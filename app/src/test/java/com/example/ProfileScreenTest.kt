package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.ProfileTabContent
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun app_rendersWithoutCrashing() {
        composeTestRule.setContent {
            val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
            val db = com.example.data.local.AppDatabase.getDatabase(context)
            val repo = com.example.data.repository.PostRepository(db.postDao())
            val viewModel = com.example.ui.viewmodel.PostViewModel(context, repo)

            MyApplicationTheme {
                ProfileTabContent(
                    viewModel = viewModel,
                    myProfile = com.example.data.model.UserProfileEntity(username = "You"),
                    allFollows = emptyList(),
                    reputations = mapOf("You" to 0),
                    advancedReputations = emptyMap(),
                    posts = emptyList()
                )
            }
        }
    }
}
