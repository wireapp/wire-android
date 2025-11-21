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
import com.ramcosta.composedestinations.animations.rememberNavHostEngine
import com.ramcosta.composedestinations.manualcomposablecalls.composable
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.scope.resultBackNavigator
import com.ramcosta.composedestinations.scope.resultRecipient
import com.ramcosta.composedestinations.spec.Route
import com.wire.android.feature.sketch.destinations.DrawingCanvasScreenDestination
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
    startDestination: Route,
    modifier: Modifier = Modifier,
) {
    val navHostEngine = rememberNavHostEngine()

    AdjustDestinationStylesForTablets()
    DestinationsNavHost(
        modifier = modifier,
        navGraph = WireMainNavGraph,
        engine = navHostEngine,
        startRoute = startDestination,
        navController = navigator.navController,
        dependenciesContainerBuilder = {
            // 👇 To make Navigator available to all destinations as a non-navigation parameter
            dependency(navigator)

            // 👇 To make LoginTypeSelector available to all destinations as a non-navigation parameter if provided
            if (loginTypeSelector != null) dependency(loginTypeSelector)

            // 👇 To tie NewConversationViewModel to nested NewConversationNavGraph, making it shared between all screens that belong to it
            dependency(NavGraphs.newConversation) {
                val parentEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry(NavGraphs.newConversation.route)
                }
                hiltViewModel<NewConversationViewModel>(parentEntry)
            }

            // 👇 To reuse LoginEmailViewModel from NewLoginPasswordScreen on NewLoginVerificationCodeScreen
            dependency(NewLoginVerificationCodeScreenDestination) {
                val loginPasswordEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry(NewLoginPasswordScreenDestination.route)
                }
                hiltViewModel<LoginEmailViewModel>(loginPasswordEntry)
            }

            // 👇 To tie SSOUrlConfigHolder to nested LoginNavGraph, making it shared between all screens that belong to it
            dependency(NavGraphs.login) {
                val parentEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry(NavGraphs.login.route)
                }
                SSOUrlConfigHolderImpl(parentEntry.savedStateHandle)
            }

            // 👇 To tie SSOUrlConfigHolder to nested NewLoginNavGraph, making it shared between all screens that belong to it
            dependency(NavGraphs.newLogin) {
                val parentEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry(NavGraphs.newLogin.route)
                }
                SSOUrlConfigHolderImpl(parentEntry.savedStateHandle)
            }

            // 👇 To tie TeamMigrationViewModel to PersonalToTeamMigrationNavGraph, making it shared between all screens that belong to it
            dependency(NavGraphs.personalToTeamMigration) {
                val parentEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry(NavGraphs.personalToTeamMigration.route)
                }
                hiltViewModel<TeamMigrationViewModel>(parentEntry)
            }
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
