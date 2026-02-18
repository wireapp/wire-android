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
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.manualcomposablecalls.composable
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.scope.resultBackNavigator
import com.ramcosta.composedestinations.scope.resultRecipient
import com.ramcosta.composedestinations.spec.Route
import com.wire.android.feature.cells.ui.LocalSharedTransitionScope
import com.wire.android.feature.sketch.destinations.DrawingCanvasScreenDestination
import com.wire.android.feature.sketch.model.DrawingCanvasNavBackArgs
import com.wire.android.navigation.style.DefaultNestedNavGraphAnimations
import com.wire.android.navigation.style.DefaultRootNavGraphAnimations
import com.wire.android.ui.NavGraphs
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel
import com.wire.android.ui.authentication.login.sso.SSOUrlConfigHolderImpl
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.NewLoginPasswordScreenDestination
import com.wire.android.ui.destinations.NewLoginVerificationCodeScreenDestination
import com.wire.android.ui.home.conversations.ConversationScreen
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.userprofile.teammigration.TeamMigrationViewModel

@OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainNavHost(
    navigator: Navigator,
    loginTypeSelector: LoginTypeSelector?,
    startDestination: Route,
    modifier: Modifier = Modifier,
) {
    val navHostEngine = rememberAnimatedNavHostEngine(
        rootDefaultAnimations = DefaultRootNavGraphAnimations,
        defaultAnimationsForNestedNavGraph = mapOf(
            NavGraphs.createPersonalAccount to DefaultNestedNavGraphAnimations,
            NavGraphs.createTeamAccount to DefaultNestedNavGraphAnimations,
            NavGraphs.newConversation to DefaultNestedNavGraphAnimations,
        )
    )

    AdjustDestinationStylesForTablets()
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            DestinationsNavHost(
                modifier = Modifier,
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
                     * In compose-destinations v1 it's not possible for code generation to use destination generated in another module,
                     * that's why it's necessary to use "open" approach and manually call the composable function for the destination.
                     * In v2 this will be possible, so that we will be able to use regular `ResultRecipient` instead of `OpenResultRecipient`
                     * and provide it directly inside the destination's composable without the need to passing it manually.
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
    }
}
