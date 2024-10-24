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
package com.wire.android.ui.userprofile.teammigration

import android.graphics.drawable.ColorDrawable
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.dependency
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.TeamMigrationDestination
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.rememberNavigator
import com.wire.android.navigation.rememberTrackingAnimatedNavController
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.NavGraphs
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.userprofile.teammigration.common.ConfirmMigrationLeaveDialog

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class)
@WireDestination(style = PopUpNavigationAnimation::class)
@Composable
fun TeamMigrationScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    teamMigrationViewModel: TeamMigrationViewModel = hiltViewModel()
) {
    val navHostEngine = rememberAnimatedNavHostEngine(
        rootDefaultAnimations = RootNavGraphDefaultAnimations.ACCOMPANIST_FADING
    )
    val navController = rememberTrackingAnimatedNavController {
        TeamMigrationDestination.fromRoute(it)?.itemName
    }

    val isRunInPreview = LocalInspectionMode.current

    if (!isRunInPreview) {
        val activity = LocalActivity.current
        activity.window.setBackgroundDrawable(
            ColorDrawable(colorsScheme().windowPersonalToTeamMigration.toArgb())
        )
    }

    Column(
        modifier = modifier
            .padding(top = dimensions().spacing32x)
            .clip(
                shape = RoundedCornerShape(
                    dimensions().corner16x,
                    dimensions().corner16x
                )
            )
            .fillMaxSize()
            .background(color = colorsScheme().surface)
    ) {
        IconButton(
            modifier = Modifier.align(alignment = Alignment.End),
            onClick = {
                // If the user completed team migration, we don't need to show the dialog
                if (navController.currentDestination?.route == NavGraphs.personalToTeamMigration.destinations.last().route) {
                    navigator.navigateBack()
                } else {
                    teamMigrationViewModel.sendPersonalToTeamMigrationDismissed()
                    teamMigrationViewModel.showMigrationLeaveDialog()
                }
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = stringResource(R.string.personal_to_team_migration_close_icon_content_description)
            )
        }

        DestinationsNavHost(
            navGraph = NavGraphs.personalToTeamMigration,
            engine = navHostEngine,
            navController = navController,
            dependenciesContainerBuilder = {
                dependency(navigator)
                dependency(NavGraphs.personalToTeamMigration) {
                    val parentEntry = remember(navBackStackEntry) {
                        navController.getBackStackEntry(NavGraphs.personalToTeamMigration.route)
                    }
                    hiltViewModel<TeamMigrationViewModel>(parentEntry)
                }
            }
        )
    }

    if (teamMigrationViewModel.teamMigrationState.shouldShowMigrationLeaveDialog) {
        ConfirmMigrationLeaveDialog(
            onContinue = {
                teamMigrationViewModel.sendPersonalTeamCreationFlowCanceledEvent(
                    modalContinueClicked = true
                )
                teamMigrationViewModel.hideMigrationLeaveDialog()
            }
        ) {
            teamMigrationViewModel.hideMigrationLeaveDialog()
            teamMigrationViewModel.sendPersonalTeamCreationFlowCanceledEvent(
                modalLeaveClicked = true
            )
            navigator.navigateBack()
        }
    }
}

@MultipleThemePreviews
@Composable
private fun TeamMigrationScreenPreview() {
    WireTheme {
        TeamMigrationScreen(navigator = rememberNavigator { })
    }
}
