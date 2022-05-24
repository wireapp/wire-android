package com.wire.android.ui

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.wire.android.ui.home.conversations.ConversationScreen
import com.wire.android.ui.home.conversations.ConversationViewModel
import com.wire.android.ui.theme.WireTheme
import com.wire.android.utils.WorkManagerTestRule
import com.wire.android.utils.getViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class
)
@HiltAndroidTest
class ConversationScreenTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    // Second, as we are using a WorkManager
    // In an instrumented test we need to ensure this gets initialized before launching any Compose/Activity Rule
    @get:Rule(order = 1)
    var workManagerTestRule = WorkManagerTestRule()

    // Third, we create the compose rule using an AndroidComposeRule, as we are depending on instrumented environment ie: Hilt, WorkManager
    @get:Rule(order = 2)
    val composeTestRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<WireActivity>

    @Before
    fun setUp() {
        hiltRule.inject()

        // Start the app
        scenario = ActivityScenario.launch(Intent(ApplicationProvider.getApplicationContext(), WireActivity::class.java))
        scenario.onActivity { activity ->
            activity.setContent {
                WireTheme {
                    ConversationScreen(getViewModel(activity, ConversationViewModel::class))
                }
            }
        }
    }

    @Test
    @Ignore("Ignored until we know how to pass a conversationId to this viewmodel")
    fun userSearchesConversation() {
        composeTestRule.onNodeWithText("Conversations").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Conversation search icon").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Conversation search icon").performTextInput("Conv")
    }
}
