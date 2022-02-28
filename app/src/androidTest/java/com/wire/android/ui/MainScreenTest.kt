package com.wire.android.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.filters.LargeTest
import com.wire.android.ui.authentication.welcome.WelcomeScreen
import com.wire.android.ui.theme.WireTheme
import org.junit.Rule
import org.junit.Test


@LargeTest
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun MyTest() {
        // Start the app
        composeTestRule.setContent {
            WireTheme {
                val navController = rememberNavController()
                WelcomeScreen(navController)
            }
        }

        composeTestRule.onNodeWithText("Login").assertIsDisplayed()

//        composeTestRule.onNodeWithText("Login").performClick()

    }
}
