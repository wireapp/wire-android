/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.manualcomposablecalls.composable
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.scope.resultBackNavigator
import com.ramcosta.composedestinations.scope.resultRecipient
import com.ramcosta.composedestinations.spec.Direction
import com.wire.android.ui.sketch.destinations.DrawingCanvasScreenDestination
import com.wire.android.feature.sketch.model.DrawingCanvasNavBackArgs
import com.wire.android.ui.NavGraphs
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel
import com.wire.android.ui.authentication.login.sso.SSOUrlConfigHolderImpl
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.NewLoginPasswordScreenDestination
import com.wire.android.ui.destinations.NewLoginVerificationCodeScreenDestination
import com.wire.android.ui.home.conversations.ConversationScreen
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.userprofile.teammigration.TeamMigrationViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainNavHost(
    navigator: Navigator,
    loginTypeSelector: LoginTypeSelector?,
    startDestination: Direction,
    modifier: Modifier = Modifier,
) {
    AdjustDestinationStylesForTablets()
    DestinationsNavHost(
        modifier = modifier,
        navGraph = NavGraphs.wireRoot,
        start = startDestination,
        navController = navigator.navController,
        dependenciesContainerBuilder = {
            // 👇 To make Navigator available to all destinations as a non-navigation parameter
            dependency(navigator)

            // 👇 To make LoginTypeSelector available to all destinations as a non-navigation parameter if provided
            if (loginTypeSelector != null) dependency(loginTypeSelector)

            // Note: In v2, graph-scoped dependencies are handled differently.
            // ViewModels are automatically scoped to their nav graph back stack entries.
            // Manual scoping is done in the destination composables themselves using hiltViewModel(parentEntry).
        },
        manualComposableCallsBuilder = {
            /**
             * Manual composable call is needed here to pass ResultRecipients from different modules.
             * This approach allows us to handle results from destinations in other modules
             * without requiring those modules to have direct dependencies on each other.
             * https://github.com/raamcosta/compose-destinations/issues/508#issuecomment-1883166574
             */
            composable(ConversationScreenDestination) {
                ConversationScreen(
                    navigator = navigator,
                    groupDetailsScreenResultRecipient = resultRecipient(),
                    mediaGalleryScreenResultRecipient = resultRecipient(),
                    imagePreviewScreenResultRecipient = resultRecipient(),
                    drawingCanvasScreenResultRecipient = resultRecipient<DrawingCanvasScreenDestination, DrawingCanvasNavBackArgs>(),
                    resultNavigator = resultBackNavigator(),
                )
            }
        }
    )
}
