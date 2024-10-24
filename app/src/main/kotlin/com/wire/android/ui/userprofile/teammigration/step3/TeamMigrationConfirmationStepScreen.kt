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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.wire.android.R
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.ui.common.WireCheckbox
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.destinations.TeamMigrationDoneStepScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.teammigration.common.BottomLineButtons
import com.wire.android.ui.userprofile.teammigration.common.BulletList
import com.wire.android.ui.userprofile.teammigration.PersonalToTeamMigrationNavGraph
import com.wire.android.ui.userprofile.teammigration.TeamMigrationViewModel
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes

@PersonalToTeamMigrationNavGraph
@WireDestination(
    style = SlideNavigationAnimation::class
)
@Composable
fun TeamMigrationConfirmationStepScreen(
    navigator: DestinationsNavigator,
    teamMigrationViewModel: TeamMigrationViewModel
) {
    TeamMigrationConfirmationStepScreenContent(
        onContinueButtonClicked = {
            // TODO: call the API to migrate the user to the team, if successful navigate to next screen
            navigator.navigate(TeamMigrationDoneStepScreenDestination)
        },
        onBackPressed = {
            navigator.popBackStack()
        },
        passwordTextState = teamMigrationViewModel.teamMigrationState.passwordTextState
    )
}

@Composable
private fun TeamMigrationConfirmationStepScreenContent(
    passwordTextState: TextFieldState,
    modifier: Modifier = Modifier,
    onContinueButtonClicked: () -> Unit = { },
    onBackPressed: () -> Unit = { }
) {

    val agreedToMigrationTerms = remember {
        mutableStateOf(false)
    }

    val acceptedWireTermsOfUse = remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    start = dimensions().spacing16x,
                    end = dimensions().spacing16x
                )
                .verticalScroll(rememberScrollState())
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

            PasswordInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = dimensions().spacing56x,
                        bottom = dimensions().spacing56x
                    ),
                passwordState = passwordTextState,
            )
            Row {
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
        val isContinueButtonEnabled =
            passwordTextState.text.isNotEmpty() && agreedToMigrationTerms.value && acceptedWireTermsOfUse.value
        BottomLineButtons(
            isContinueButtonEnabled = isContinueButtonEnabled,
            onContinue = onContinueButtonClicked,
            onBack = onBackPressed
        )
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

@Composable
private fun PasswordInput(
    passwordState: TextFieldState,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    WirePasswordTextField(
        textState = passwordState,
        labelText = stringResource(R.string.personal_to_team_migration_confirmation_step_password_field_label),
        keyboardOptions = KeyboardOptions.DefaultPassword,
        placeholderText = stringResource(R.string.personal_to_team_migration_confirmation_step_password_field_placeholder),
        onKeyboardAction = {
            keyboardController?.hide()
        },
        modifier = modifier
            .testTag("passwordFieldTeamMigration"),
        testTag = "passwordFieldTeamMigration"
    )
}

@PreviewMultipleThemes
@Composable
private fun TeamMigrationConfirmationStepPreview() {
    WireTheme {
        TeamMigrationConfirmationStepScreenContent(
            passwordTextState = rememberTextFieldState()
        )
    }
}
