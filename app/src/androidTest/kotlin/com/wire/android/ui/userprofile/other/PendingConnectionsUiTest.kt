/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.userprofile.other

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.model.Clickable
import com.wire.android.model.ItemActionType
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.WireTestTheme
import com.wire.android.ui.common.connection.CONNECTION_ACTION_BUTTONS_TEST_TAG
import com.wire.android.ui.home.conversations.search.InternalContactSearchResultItem
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireColorSchemeTypes
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.ContactStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class PendingConnectionsUiTest(private val dark: Boolean, private val fontScale: Float) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun pendingRowsAllowProfileNavigationButPreventSelectionAndConversationActions() {
        var openedProfile = false
        var selected = false
        var openedConversation = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                WireTestTheme(wireColorScheme = if (dark) WireColorSchemeTypes.dark else WireColorSchemeTypes.light) {
                    Surface {
                        Column {
                            InternalContactSearchResultItem(
                                avatarData = UserAvatarData(), name = "Isabel Teammate", label = "@kim.dawson",
                                membership = Membership.Standard, searchQuery = "", connectionState = ConnectionState.ACCEPTED,
                                onCheckClickable = Clickable { selected = true }, isSelected = false,
                                clickable = Clickable { openedProfile = true }, actionType = ItemActionType.CLICK,
                                isOnboardingPending = true, modifier = Modifier.testTag("profile_row")
                            )
                            InternalContactSearchResultItem(
                                avatarData = UserAvatarData(), name = "Pending group member", label = "@pending",
                                membership = Membership.Standard, searchQuery = "", connectionState = ConnectionState.ACCEPTED,
                                onCheckClickable = Clickable { selected = true }, isSelected = false,
                                clickable = Clickable {}, actionType = ItemActionType.CHECK,
                                isOnboardingPending = true, modifier = Modifier.testTag("picker_row")
                            )
                            OnboardingPendingInfo()
                            ContentFooter(
                                state = OtherUserStubs.provideState().copy(contactStatus = ContactStatus.NON_CONTACTABLE),
                                onOpenConversation = { openedConversation = true }
                            )
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("profile_row").performClick()
        assertTrue(openedProfile)
        compose.onNodeWithTag("picker_row").assertIsNotEnabled().performTouchInput { click() }
        compose.onRoot().performKeyInput {
            keyDown(Key.Tab)
            keyUp(Key.Tab)
            keyDown(Key.Enter)
            keyUp(Key.Enter)
        }
        assertFalse(selected)
        compose.onAllNodesWithText("Onboarding pending", useUnmergedTree = true).assertCountEquals(3)
        compose.onNodeWithTag(CONNECTION_ACTION_BUTTONS_TEST_TAG).assertIsNotEnabled().performTouchInput { click() }
        assertFalse(openedConversation)
        compose.onNodeWithText("You can start a conversation with this team member once they are fully onboarded.").assertExists()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir, "pending-connections-$dark-$fontScale.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun normalRowsRetainNavigationAndSelectionWithoutPendingText() {
        var openedProfile = false
        var selected = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                WireTestTheme(wireColorScheme = if (dark) WireColorSchemeTypes.dark else WireColorSchemeTypes.light) {
                    Surface {
                        Column {
                            InternalContactSearchResultItem(
                                avatarData = UserAvatarData(), name = "Isabel Teammate", label = "kim.dawson",
                                membership = Membership.Standard, searchQuery = "", connectionState = ConnectionState.ACCEPTED,
                                onCheckClickable = Clickable {}, isSelected = false,
                                clickable = Clickable { openedProfile = true }, actionType = ItemActionType.CLICK,
                                isOnboardingPending = false, modifier = Modifier.testTag("normal_profile_row")
                            )
                            InternalContactSearchResultItem(
                                avatarData = UserAvatarData(), name = "Group member", label = "member",
                                membership = Membership.Standard, searchQuery = "", connectionState = ConnectionState.ACCEPTED,
                                onCheckClickable = Clickable { selected = true }, isSelected = false,
                                clickable = Clickable {}, actionType = ItemActionType.CHECK,
                                isOnboardingPending = false, modifier = Modifier.testTag("normal_picker_row")
                            )
                        }
                    }
                }
            }
        }
        compose.onAllNodesWithText("Onboarding pending", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithText("@kim.dawson").assertExists()
        compose.onNodeWithTag("normal_profile_row").performClick()
        assertTrue(openedProfile)
        compose.onNodeWithTag("normal_picker_row").assertIsEnabled().performTouchInput { click() }
        assertTrue(selected)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "dark={0}, fontScale={1}")
        fun themesAndFontScales() = listOf(arrayOf<Any>(false, 1f), arrayOf<Any>(true, 1f), arrayOf<Any>(false, 2f), arrayOf<Any>(true, 2f))
    }
}
