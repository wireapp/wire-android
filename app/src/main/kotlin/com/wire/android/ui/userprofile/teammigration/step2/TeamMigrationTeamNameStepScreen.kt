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
package com.wire.android.ui.userprofile.teammigration.step2
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireRootNavGraph

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.AuthSlideNavigationAnimation
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.destinations.TeamMigrationConfirmationStepScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.teammigration.PersonalToTeamMigrationNavGraph
import com.wire.android.ui.userprofile.teammigration.TeamMigrationViewModel
import com.wire.android.ui.userprofile.teammigration.common.BottomLineButtons
import com.wire.android.ui.userprofile.teammigration.common.TeamMigrationContainer
import com.wire.android.util.ui.PreviewMultipleThemes

@PersonalToTeamMigrationNavGraph
@Destination<WireRootNavGraph>(
    style = AuthSlideNavigationAnimation::class
)
@Composable
fun TeamMigrationTeamNameStepScreen(
    navigator: Navigator,
    teamMigrationViewModel: TeamMigrationViewModel
) {
    TeamMigrationTeamNameStepScreenContent(
        onContinueButtonClicked = {
            navigator.navigate(NavigationCommand(TeamMigrationConfirmationStepScreenDestination))
        },
        onBackButtonClicked = navigator::navigateBack,
        teamNameTextFieldState = teamMigrationViewModel.teamMigrationState.teamNameTextState
    )
    LaunchedEffect(Unit) {
        teamMigrationViewModel.setCurrentStep(TeamMigrationViewModel.TEAM_MIGRATION_TEAM_NAME_STEP)
    }
}

@Composable
private fun TeamMigrationTeamNameStepScreenContent(
    teamNameTextFieldState: TextFieldState,
    modifier: Modifier = Modifier,
    onContinueButtonClicked: () -> Unit = { },
    onBackButtonClicked: () -> Unit = { }
) {
    TeamMigrationContainer(
        onClose = onBackButtonClicked,
        closeIconContentDescription = stringResource(R.string.personal_to_team_migration_close_team_name_content_description),
        showConfirmationDialogWhenClosing = true,
        bottomBar = {
            val isContinueButtonEnabled = teamNameTextFieldState.text.isNotEmpty() && teamNameTextFieldState.text.isNotBlank()
            BottomLineButtons(
                isContinueButtonEnabled = isContinueButtonEnabled,
                onContinue = onContinueButtonClicked,
                backButtonContentDescription = stringResource(
                    R.string.personal_to_team_migration_back_button_team_name_content_description
                ),
                onBack = onBackButtonClicked
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = dimensions().spacing16x,
                    end = dimensions().spacing16x
                )
        ) {
            Text(
                modifier = Modifier
                    .padding(top = dimensions().spacing24x)
                    .align(alignment = Alignment.CenterHorizontally),
                text = stringResource(R.string.personal_to_team_migration_step_label, 2),
                style = MaterialTheme.wireTypography.subline01,
                color = colorsScheme().secondaryText,
            )
            Text(
                modifier = Modifier
                    .padding(
                        top = dimensions().spacing8x,
                        bottom = dimensions().spacing56x
                    )
                    .align(alignment = Alignment.CenterHorizontally),
                text = stringResource(R.string.personal_to_team_migration_team_name_step),
                style = MaterialTheme.wireTypography.title01,
                color = colorsScheme().onBackground
            )

            Text(
                text = stringResource(R.string.personal_to_team_migration_team_name_description),
                style = MaterialTheme.wireTypography.body01,
                color = colorsScheme().onBackground
            )

            TeamNameInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensions().spacing24x),
                textFieldState = teamNameTextFieldState,
            )
        }
    }
}

@Composable
private fun TeamNameInput(
    textFieldState: TextFieldState,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    WireTextField(
        textState = textFieldState,
        labelText = stringResource(R.string.personal_to_team_migration_team_name_step),
        onKeyboardAction = {
            keyboardController?.hide()
        },
        placeholderText = stringResource(R.string.personal_to_team_migration_team_name_text_field_placeholder),
        modifier = modifier
            .testTag("teamNameFieldTeamMigration"),
        testTag = "teamNameFieldTeamMigration"
    )
}

@PreviewMultipleThemes
@Composable
private fun TeamMigrationTeamNameStepScreenPreview() {
    WireTheme {
        TeamMigrationTeamNameStepScreenContent(
            teamNameTextFieldState = rememberTextFieldState("Your Team")
        )
    }
}
