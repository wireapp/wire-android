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
package com.wire.android.ui.userprofile.teammigration.step1

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.navigation.style.AuthPopUpNavigationAnimation
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.destinations.TeamMigrationTeamNameStepScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.teammigration.PersonalToTeamMigrationNavGraph
import com.wire.android.ui.userprofile.teammigration.TeamMigrationViewModel
import com.wire.android.ui.userprofile.teammigration.common.BottomLineButtons
import com.wire.android.ui.userprofile.teammigration.common.TeamMigrationContainer
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes

@PersonalToTeamMigrationNavGraph(start = true)
@WireDestination(
    style = AuthPopUpNavigationAnimation::class
)
@Composable
fun TeamMigrationTeamPlanStepScreen(
    navigator: Navigator,
    teamMigrationViewModel: TeamMigrationViewModel
) {
    TeamMigrationTeamPlanStepScreenContent(
        onBackButtonClicked = navigator::navigateBack,
        onContinueButtonClicked = {
            navigator.navigate(NavigationCommand(TeamMigrationTeamNameStepScreenDestination))
        }
    )

    LaunchedEffect(Unit) {
        teamMigrationViewModel.setCurrentStep(TeamMigrationViewModel.TEAM_MIGRATION_TEAM_PLAN_STEP)
    }
}

@Composable
private fun TeamMigrationTeamPlanStepScreenContent(
    modifier: Modifier = Modifier,
    onContinueButtonClicked: () -> Unit = { },
    onBackButtonClicked: () -> Unit = { }
) {
    TeamMigrationContainer(
        onClose = onBackButtonClicked,
        closeIconContentDescription = stringResource(R.string.personal_to_team_migration_close_team_account_content_description),
        showConfirmationDialogWhenClosing = true,
        bottomBar = {
            BottomLineButtons(
                isContinueButtonEnabled = true,
                isBackButtonVisible = false,
                onContinue = onContinueButtonClicked
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = dimensions().spacing16x,
                    end = dimensions().spacing16x
                )
        ) {
            Text(
                modifier = Modifier
                    .padding(top = dimensions().spacing24x)
                    .align(alignment = Alignment.CenterHorizontally),
                text = stringResource(R.string.personal_to_team_migration_step_label, 1),
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
                text = stringResource(R.string.personal_to_team_migration_team_plan_step),
                style = MaterialTheme.wireTypography.title01,
                color = colorsScheme().onBackground
            )

            Text(
                text = stringResource(R.string.personal_to_team_migration_team_plan_description),
                style = MaterialTheme.wireTypography.body01,
                color = colorsScheme().onBackground
            )
            AdvantagesList()
            LearnMoreWirePlans()
        }
    }
}

@Composable
private fun LearnMoreWirePlans(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val annotatedString = buildAnnotatedString {
        val wirePlansLink =
            stringResource(R.string.url_wire_plans)
        withLink(
            link = LinkAnnotation.Clickable(
                tag = "plans",
                styles = TextLinkStyles(
                    SpanStyle(
                        color = colorsScheme().onBackground,
                        textDecoration = TextDecoration.Underline
                    ),
                ),
                linkInteractionListener = {
                    CustomTabsHelper.launchUrl(context, wirePlansLink)
                },
            ),
        ) {
            append(stringResource(R.string.personal_to_team_migration_team_plan_learn_more_about_wire_plan))
        }
    }
    Text(
        modifier = modifier.padding(top = dimensions().spacing24x),
        text = annotatedString,
        style = MaterialTheme.wireTypography.body01,
        color = colorsScheme().onBackground
    )
}

@Composable
private fun AdvantagesList(
    modifier: Modifier = Modifier
) {
    val texts = listOf(
        Pair(
            stringResource(R.string.personal_to_team_migration_team_plan_admin_console_label),
            stringResource(R.string.personal_to_team_migration_team_plan_admin_console_details),
        ),
        Pair(
            stringResource(R.string.personal_to_team_migration_team_plan_effortless_collaboration_label),
            stringResource(R.string.personal_to_team_migration_team_plan_effortless_collaboration_details),
        ),
        Pair(
            stringResource(R.string.personal_to_team_migration_team_plan_larger_meetings_label),
            stringResource(R.string.personal_to_team_migration_team_plan_larger_meetings_details),
        ),
        Pair(
            stringResource(R.string.personal_to_team_migration_team_plan_availability_status_label),
            stringResource(R.string.personal_to_team_migration_team_plan_availability_status_details),
        ),
        Pair(
            stringResource(R.string.personal_to_team_migration_team_plan_upgrade_to_enterprise_label),
            stringResource(R.string.personal_to_team_migration_team_plan_upgrade_to_enterprise_description),
        ),
    )
    Column(modifier = modifier) {
        texts.forEach { (title, description) ->
            val annotatedString = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(title)
                }
                append(" ")
                append(description)
            }
            AdvantageRow(annotatedString)
        }
    }
}

@Composable
private fun AdvantageRow(
    annotatedString: AnnotatedString,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(
                top = dimensions().spacing24x,
                bottom = dimensions().spacing12x
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = null,
                tint = colorsScheme().positive,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Text(
                text = annotatedString,
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier.padding(
                    start = dimensions().spacing12x
                )
            )
        }
        Divider(
            color = colorsScheme().outline
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun TeamMigrationTeamPlanStepScreenPreview() {
    WireTheme {
        TeamMigrationTeamPlanStepScreenContent()
    }
}
