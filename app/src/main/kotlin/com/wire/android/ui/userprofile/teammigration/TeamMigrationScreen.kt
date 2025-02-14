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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
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
import com.wire.android.ui.userprofile.teammigration.step1.TEAM_MIGRATION_TEAM_PLAN_STEP
import com.wire.android.ui.userprofile.teammigration.step2.TEAM_MIGRATION_TEAM_NAME_STEP
import com.wire.android.ui.userprofile.teammigration.step3.TEAM_MIGRATION_CONFIRMATION_STEP
import com.wire.android.ui.userprofile.teammigration.step4.TEAM_MIGRATION_DONE_STEP

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
    // TODO: after updating material3 to 1.4.0, we can replace it by bottom sheet with `sheetGestureEnabled = false`
    //  so that it can't be dragged down and the scrim looks way better than now and covers also status bar properly
    if (!isRunInPreview) {
        val activity = LocalActivity.current
        activity.window.setBackgroundDrawable(
            ColorDrawable(colorsScheme().scrim.toArgb())
        )
    }

    Column(
        modifier = modifier
            .padding(top = dimensions().spacing32x)
            .navigationBarsPadding()
            .clip(
                shape = RoundedCornerShape(
                    dimensions().corner16x,
                    dimensions().corner16x
                )
            )
            .fillMaxSize()
            .background(color = colorsScheme().surface)
    ) {
        val closeIconContentDescription = when (teamMigrationViewModel.teamMigrationState.currentStep) {
            TEAM_MIGRATION_TEAM_PLAN_STEP -> stringResource(R.string.personal_to_team_migration_close_team_account_content_description)
            TEAM_MIGRATION_TEAM_NAME_STEP -> stringResource(R.string.personal_to_team_migration_close_team_name_content_description)
            TEAM_MIGRATION_CONFIRMATION_STEP -> stringResource(R.string.personal_to_team_migration_close_confirmation_content_description)
            TEAM_MIGRATION_DONE_STEP -> stringResource(R.string.personal_to_team_migration_close_team_created_content_description)
            else -> stringResource(R.string.personal_to_team_migration_close_icon_content_description)
        }

        IconButton(
            modifier = Modifier.align(alignment = Alignment.End),
            onClick = {
                // If the user completed team migration, we don't need to show the dialog
                if (navController.currentDestination?.route == NavGraphs.personalToTeamMigration.destinations.last().route) {
                    navigator.navigateBack()
                } else {
                    teamMigrationViewModel.showMigrationLeaveDialog()
                }
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = closeIconContentDescription
            )
        }

        DestinationsNavHost(
            navGraph = NavGraphs.personalToTeamMigration,
            engine = navHostEngine,
            navController = navController,
            dependenciesContainerBuilder = {
                dependency(navigator)
                dependency(NavGraphs.personalToTeamMigration) {
                    teamMigrationViewModel
                }
            }
        )
    }

    if (teamMigrationViewModel.teamMigrationState.shouldShowMigrationLeaveDialog) {
        ConfirmMigrationLeaveDialog(
            onContinue = teamMigrationViewModel::hideMigrationLeaveDialog
        ) {
            teamMigrationViewModel.hideMigrationLeaveDialog()
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
