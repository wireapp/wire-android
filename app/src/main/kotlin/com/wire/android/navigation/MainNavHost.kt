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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.app.destinations.ConversationScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.NewLoginPasswordScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.NewLoginVerificationCodeScreenDestination
import com.ramcosta.composedestinations.generated.app.navgraphs.NewConversationGraph
import com.ramcosta.composedestinations.generated.app.navgraphs.PersonalToTeamMigrationGraph
import com.ramcosta.composedestinations.generated.app.navgraphs.WireRootGraph
import com.ramcosta.composedestinations.generated.app.navtype.groupConversationDetailsNavBackArgsNavType
import com.ramcosta.composedestinations.generated.app.navtype.imagesPreviewNavBackArgsNavType
import com.ramcosta.composedestinations.generated.app.navtype.mediaGalleryNavBackArgsNavType
import com.ramcosta.composedestinations.generated.sketch.destinations.DrawingCanvasScreenDestination
import com.ramcosta.composedestinations.generated.sketch.navtype.drawingCanvasNavBackArgsNavType
import com.ramcosta.composedestinations.manualcomposablecalls.composable
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.navigation.destination
import com.ramcosta.composedestinations.navigation.navGraph
import com.ramcosta.composedestinations.scope.resultBackNavigator
import com.ramcosta.composedestinations.scope.resultRecipient
import com.ramcosta.composedestinations.spec.Direction
import com.wire.android.feature.sketch.model.DrawingCanvasNavBackArgs
import com.wire.android.navigation.transition.LocalSharedTransitionScope
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel
import com.wire.android.ui.home.conversations.ConversationScreen
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.userprofile.teammigration.TeamMigrationViewModel

@OptIn(ExperimentalAnimationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainNavHost(
    navigator: Navigator,
    loginTypeSelector: LoginTypeSelector?,
    startDestination: Direction,
    modifier: Modifier = Modifier,
) {
    val navHostEngine = rememberWireNavHostEngine(Alignment.Center)
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            DestinationsNavHost(
                modifier = Modifier,
                navGraph = WireRootGraph,
                defaultTransitions = WireRootGraph.defaultTransitions,
                engine = navHostEngine,
                start = startDestination,
                navController = navigator.navController,
                dependenciesContainerBuilder = {
                    // 👇 To make Navigator available to all destinations as a non-navigation parameter
                    dependency(navigator)

                    // 👇 To make LoginTypeSelector available to all destinations as a non-navigation parameter if provided
                    if (loginTypeSelector != null) dependency(loginTypeSelector)

                    // 👇 To tie NewConversationViewModel to nested NewConversationNavGraph,
                    // making it shared between all screens that belong to it
                    navGraph(NewConversationGraph) {
                        val parentEntry = remember(navBackStackEntry) {
                            navController.getBackStackEntry(NewConversationGraph.route)
                        }
                        dependency(hiltViewModel<NewConversationViewModel>(parentEntry))
                    }

                    // 👇 To reuse LoginEmailViewModel from NewLoginPasswordScreen on NewLoginVerificationCodeScreen
                    destination(NewLoginVerificationCodeScreenDestination) {
                        val loginPasswordEntry = remember(navBackStackEntry) {
                            navController.getBackStackEntry(NewLoginPasswordScreenDestination.route)
                        }
                        dependency(hiltViewModel<LoginEmailViewModel>(loginPasswordEntry))
                    }

                    // 👇 To tie TeamMigrationViewModel to PersonalToTeamMigrationNavGraph, making it shared between all screens that belong to it
                    navGraph(PersonalToTeamMigrationGraph) {
                        val parentEntry = remember(navBackStackEntry) {
                            navController.getBackStackEntry(PersonalToTeamMigrationGraph.route)
                        }
                        dependency(hiltViewModel<TeamMigrationViewModel>(parentEntry))
                    }
                },
                manualComposableCallsBuilder = {
                    /**
                     * Keep manual composable calls for cross-module result wiring until we refactor
                     * those destinations to rely on generated dependencies directly.
                     */
                    composable(ConversationScreenDestination) {
                        ConversationScreen(
                            navigator = navigator,
                            groupDetailsScreenResultRecipient = resultRecipient(groupConversationDetailsNavBackArgsNavType),
                            mediaGalleryScreenResultRecipient = resultRecipient(mediaGalleryNavBackArgsNavType),
                            imagePreviewScreenResultRecipient = resultRecipient(imagesPreviewNavBackArgsNavType),
                            drawingCanvasScreenResultRecipient = resultRecipient<DrawingCanvasScreenDestination, DrawingCanvasNavBackArgs>(
                                drawingCanvasNavBackArgsNavType
                            ),
                            resultNavigator = resultBackNavigator(groupConversationDetailsNavBackArgsNavType),
                        )
                    }
                }
            )
        }
    }
}
