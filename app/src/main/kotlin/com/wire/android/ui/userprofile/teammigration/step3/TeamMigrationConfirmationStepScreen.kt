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
package com.wire.android.ui.userprofile.teammigration.step3

import com.wire.android.navigation.annotation.app.WirePersonalToTeamMigrationDestination
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.AuthSlideNavigationAnimation
import com.wire.android.ui.common.WireCheckbox
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.ramcosta.composedestinations.generated.app.destinations.TeamMigrationDoneStepScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.TeamMigrationTeamPlanStepScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.teammigration.TeamMigrationState
import com.wire.android.ui.userprofile.teammigration.TeamMigrationViewModel
import com.wire.android.ui.userprofile.teammigration.common.BottomLineButtons
import com.wire.android.ui.userprofile.teammigration.common.BulletList
import com.wire.android.ui.userprofile.teammigration.common.TeamMigrationContainer
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.feature.user.migration.MigrateFromPersonalToTeamFailure

@WirePersonalToTeamMigrationDestination(
    style = AuthSlideNavigationAnimation::class
)
@Composable
fun TeamMigrationConfirmationStepScreen(
    navigator: Navigator,
    teamMigrationViewModel: TeamMigrationViewModel
) {
    val state = teamMigrationViewModel.teamMigrationState

    LaunchedEffect(state.migrationCompleted) {
        if (state.migrationCompleted) {
            navigator.navigate(NavigationCommand(TeamMigrationDoneStepScreenDestination, BackStackMode.REMOVE_CURRENT_NESTED_GRAPH))
        }
    }

    TeamMigrationConfirmationStepScreenContent(
        isMigrating = state.isMigrating,
        onContinueButtonClicked = teamMigrationViewModel::migrateFromPersonalToTeamAccount,
        onBackButtonClicked = navigator::navigateBack,
    )

    HandleErrors(
        teamMigrationState = state,
        onFailureHandled = teamMigrationViewModel::failureHandled,
        goBackToFirstStep = {
            navigator.navigate(NavigationCommand(TeamMigrationTeamPlanStepScreenDestination, BackStackMode.UPDATE_EXISTED))
        }
    )

    LaunchedEffect(Unit) {
        teamMigrationViewModel.setCurrentStep(TeamMigrationViewModel.TEAM_MIGRATION_CONFIRMATION_STEP)
    }
}

@Composable
private fun HandleErrors(
    teamMigrationState: TeamMigrationState,
    onFailureHandled: () -> Unit,
    goBackToFirstStep: () -> Unit,
) {
    val failure = teamMigrationState.migrationFailure ?: return

    when (failure) {
        is MigrateFromPersonalToTeamFailure.UserAlreadyInTeam -> {
            ErrorDialog(
                title = stringResource(R.string.personal_to_team_migration_error_title_already_in_team),
                message = stringResource(R.string.personal_to_team_migration_error_message_already_in_team),
                buttonText = stringResource(id = R.string.label_ok),
                onDismiss = {
                    onFailureHandled()
                }
            )
        }

        is MigrateFromPersonalToTeamFailure.NoNetwork -> {
            ErrorDialog(
                title = stringResource(R.string.personal_to_team_migration_error_title),
                message = stringResource(R.string.personal_to_team_migration_error_message_slow_network),
                buttonText = stringResource(id = R.string.label_try_again),
                onDismiss = {
                    onFailureHandled()
                    goBackToFirstStep()
                }
            )
        }

        else -> {
            ErrorDialog(
                title = stringResource(R.string.personal_to_team_migration_error_title),
                message = stringResource(R.string.personal_to_team_migration_error_message_unknown_error),
                buttonText = stringResource(id = R.string.label_try_again),
                onDismiss = {
                    onFailureHandled()
                    goBackToFirstStep()
                }
            )
        }
    }
}

@Composable
private fun ErrorDialog(
    title: String,
    message: String,
    buttonText: String,
    onDismiss: () -> Unit,
) {
    WireDialog(
        title = title,
        text = message,
        onDismiss = onDismiss,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = buttonText,
            type = WireDialogButtonType.Primary,
        )
    )
}

@Composable
private fun TeamMigrationConfirmationStepScreenContent(
    modifier: Modifier = Modifier,
    isMigrating: Boolean = false,
    onContinueButtonClicked: () -> Unit = { },
    onBackButtonClicked: () -> Unit = { }
) {

    val agreedToMigrationTerms = remember {
        mutableStateOf(false)
    }

    val acceptedWireTermsOfUse = remember {
        mutableStateOf(false)
    }

    TeamMigrationContainer(
        onClose = onBackButtonClicked,
        closeIconContentDescription = stringResource(R.string.personal_to_team_migration_close_confirmation_content_description),
        showConfirmationDialogWhenClosing = true,
        bottomBar = {
            val isContinueButtonEnabled = agreedToMigrationTerms.value && acceptedWireTermsOfUse.value
            BottomLineButtons(
                isMigrating = isMigrating,
                isContinueButtonEnabled = isContinueButtonEnabled,
                onContinue = onContinueButtonClicked,
                backButtonContentDescription = stringResource(
                    R.string.personal_to_team_migration_back_button_confirmation_content_description
                ),
                onBack = onBackButtonClicked,
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
                text = stringResource(R.string.personal_to_team_migration_step_label, 3),
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
                text = stringResource(R.string.personal_to_team_migration_confirmation_step),
                style = MaterialTheme.wireTypography.title01,
                color = colorsScheme().onBackground
            )
            val messages = listOf(
                stringResource(R.string.personal_to_team_migration_confirmation_step_bullet_list_first_item),
                stringResource(R.string.personal_to_team_migration_confirmation_step_bullet_list_second_item),
                stringResource(R.string.personal_to_team_migration_confirmation_step_bullet_list_third_item),
            )
            BulletList(messages)

            Row(
                modifier = Modifier.padding(top = dimensions().spacing48x)
            ) {
                WireCheckbox(
                    checked = agreedToMigrationTerms.value,
                    onCheckedChange = { agreedToMigrationTerms.value = it }
                )

                Text(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    text = stringResource(R.string.personal_to_team_migration_confirmation_step_terms),
                    style = MaterialTheme.wireTypography.subline01,
                )
            }
            Row {
                WireCheckbox(
                    checked = acceptedWireTermsOfUse.value,
                    onCheckedChange = { acceptedWireTermsOfUse.value = it }
                )
                WireTermsOfUseWithLink()
            }
        }
    }
}

@Composable
private fun RowScope.WireTermsOfUseWithLink() {
    val context = LocalContext.current
    val annotatedString = buildAnnotatedString {

        append(stringResource(R.string.personal_to_team_migration_confirmation_step_wire_terms_of_use_accept_label))
        append(" ")
        val urlTermsOfUse = stringResource(R.string.url_terms_of_use_legal)
        withLink(
            link = LinkAnnotation.Clickable(
                tag = "terms",
                styles = TextLinkStyles(SpanStyle(color = colorsScheme().primary)),
                linkInteractionListener = {
                    CustomTabsHelper.launchUrl(context, urlTermsOfUse)
                },
            ),
        ) {
            append(stringResource(R.string.personal_to_team_migration_confirmation_step_wire_terms_of_use_label))
        }
    }

    Text(
        modifier = Modifier.align(Alignment.CenterVertically),
        text = annotatedString,
        style = MaterialTheme.wireTypography.subline01,
    )
}

@PreviewMultipleThemes
@Composable
private fun TeamMigrationConfirmationStepPreview() {
    WireTheme {
        TeamMigrationConfirmationStepScreenContent()
    }
}
