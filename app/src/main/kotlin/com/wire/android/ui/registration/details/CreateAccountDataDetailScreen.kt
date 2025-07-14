/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

package com.wire.android.ui.registration.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.navigation.style.AuthPopUpNavigationAnimation
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.create.common.CreateAccountNavGraph
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.common.WireCheckbox
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.DefaultEmailDone
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.destinations.CreateAccountVerificationCodeScreenDestination
import com.wire.android.ui.newauthentication.login.NewAuthContainer
import com.wire.android.ui.newauthentication.login.NewAuthHeader
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.EMPTY
import com.wire.android.util.isHostValidForAnalytics
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.configuration.server.ServerConfig

@CreateAccountNavGraph
@WireDestination(
    navArgsDelegate = CreateAccountDataNavArgs::class,
    style = AuthPopUpNavigationAnimation::class
)
@Composable
fun CreateAccountDataDetailScreen(
    navigator: Navigator,
    createAccountDataDetailViewModel: CreateAccountDataDetailViewModel = hiltViewModel()
) {
    with(createAccountDataDetailViewModel) {
        fun navigateToCodeScreen() = navigator.navigate(
            NavigationCommand(
                CreateAccountVerificationCodeScreenDestination(
                    createAccountNavArgs.copy(
                        userRegistrationInfo = createAccountNavArgs.userRegistrationInfo.copy(
                            email = emailTextState.text.toString().trim(),
                            name = nameTextState.text.toString().trim(),
                            password = passwordTextState.text.toString(),
                            teamName = String.EMPTY
                        )
                    )
                )
            )
        )

        LaunchedEffect(createAccountDataDetailViewModel.detailsState.success) {
            if (createAccountDataDetailViewModel.detailsState.success) {
                createAccountDataDetailViewModel.onCodeSentHandled()
                navigateToCodeScreen()
            }
        }

        AccountDetailsContent(
            state = detailsState,
            emailTextState = emailTextState,
            nameTextState = nameTextState,
            passwordTextState = passwordTextState,
            confirmPasswordTextState = confirmPasswordTextState,
            teamCreationUrl = teamCreationUrl() + stringResource(R.string.create_account_email_backlink_to_team_suffix_url),
            tosUrl = tosUrl(),
            onPrivacyPolicyAccepted = ::onPrivacyPolicyAccepted,
            onTermsDialogDismiss = ::onTermsDialogDismiss,
            onTermsAccept = ::onTermsAccept,
            onBackPressed = navigator::navigateBack,
            onContinuePressed = ::onDetailsContinue,
            onErrorDismiss = ::onDetailsErrorDismiss,
            serverConfig = serverConfig
        )
    }
}

@Composable
private fun AccountDetailsContent(
    state: CreateAccountDataDetailViewState,
    emailTextState: TextFieldState,
    nameTextState: TextFieldState,
    passwordTextState: TextFieldState,
    confirmPasswordTextState: TextFieldState,
    teamCreationUrl: String,
    tosUrl: String,
    onPrivacyPolicyAccepted: (Boolean) -> Unit,
    onTermsDialogDismiss: () -> Unit,
    onTermsAccept: () -> Unit,
    onBackPressed: () -> Unit,
    onContinuePressed: () -> Unit,
    onErrorDismiss: () -> Unit,
    serverConfig: ServerConfig.Links
) {
    NewAuthContainer(
        header = {
            NewAuthHeader(
                title = {
                    Text(
                        text = stringResource(id = R.string.create_personal_account_title),
                        style = MaterialTheme.wireTypography.title01
                    )
                    if (serverConfig.isOnPremises) {
                        ServerTitle(
                            serverLinks = serverConfig,
                            style = MaterialTheme.wireTypography.body01
                        )
                    }
                },
                canNavigateBack = true,
                onNavigateBack = onBackPressed
            )
        },
        contentPadding = dimensions().spacing16x,
        content = {
            val keyboardController = LocalSoftwareKeyboardController.current
            val nameFocusRequester = remember { FocusRequester() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
            ) {
                WireTextField(
                    textState = emailTextState,
                    placeholderText = stringResource(R.string.create_account_email_placeholder),
                    labelText = stringResource(R.string.create_account_email_label),
                    labelMandatoryIcon = true,
                    state = if (!state.error.isEmailError()) {
                        WireTextFieldState.Default
                    } else {
                        WireTextFieldState.Error()
                    },
                    keyboardOptions = KeyboardOptions.DefaultEmailDone,
                    onKeyboardAction = { keyboardController?.hide() },
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
                        .testTag("emailField")
                )

                AnimatedContent(state.error.isEmailError()) { isEmailError ->
                    when (isEmailError) {
                        true -> EmailErrorDetailText(state.error)
                        false -> VerticalSpace.x16()
                    }
                }

                WireTextField(
                    textState = nameTextState,
                    placeholderText = stringResource(R.string.create_account_details_name_placeholder),
                    labelText = stringResource(R.string.create_account_details_name_label),
                    labelMandatoryIcon = true,
                    state = WireTextFieldState.Default,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        autoCorrectEnabled = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .padding(
                            start = MaterialTheme.wireDimensions.spacing16x,
                            end = MaterialTheme.wireDimensions.spacing16x,
                            bottom = MaterialTheme.wireDimensions.spacing16x
                        )
                        .focusRequester(nameFocusRequester)
                        .testTag("name"),
                )

                WirePasswordTextField(
                    textState = passwordTextState,
                    placeholderText = stringResource(R.string.create_account_details_password_placeholder),
                    labelMandatoryIcon = true,
                    descriptionText = stringResource(R.string.create_account_details_password_description),
                    keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .padding(horizontal = MaterialTheme.wireDimensions.spacing16x)
                        .testTag("password"),
                    state = if (state.error is CreateAccountDataDetailViewState.DetailsError.PasswordError.InvalidPasswordError) {
                        WireTextFieldState.Error()
                    } else {
                        WireTextFieldState.Default
                    },
                    autoFill = false,
                )

                WirePasswordTextField(
                    textState = confirmPasswordTextState,
                    placeholderText = stringResource(R.string.create_account_details_password_confirm_placeholder),
                    labelText = stringResource(R.string.create_account_details_confirm_password_label),
                    labelMandatoryIcon = true,
                    keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
                    onKeyboardAction = { keyboardController?.hide() },
                    modifier = Modifier
                        .padding(
                            horizontal = MaterialTheme.wireDimensions.spacing16x,
                            vertical = MaterialTheme.wireDimensions.spacing16x
                        )
                        .testTag("confirmPassword"),
                    state = if (state.error is CreateAccountDataDetailViewState.DetailsError.PasswordError) {
                        when (state.error) {
                            CreateAccountDataDetailViewState.DetailsError.PasswordError.PasswordsNotMatchingError ->
                                WireTextFieldState.Error(stringResource(id = R.string.create_account_details_password_not_matching_error))

                            CreateAccountDataDetailViewState.DetailsError.PasswordError.InvalidPasswordError ->
                                WireTextFieldState.Error(stringResource(id = R.string.create_account_details_password_error))
                        }
                    } else {
                        WireTextFieldState.Default
                    },
                    autoFill = false,
                )

                if (serverConfig.isHostValidForAnalytics()) {
                    Row(modifier = Modifier.padding(end = MaterialTheme.wireDimensions.spacing16x)) {
                        WireCheckbox(
                            checked = state.privacyPolicyAccepted,
                            onCheckedChange = onPrivacyPolicyAccepted,
                        )
                        WirePrivacyPolicyLink()
                    }
                }
            }

            LaunchedEffect(Unit) {
                nameFocusRequester.requestFocus()
                keyboardController?.show()
            }

            WirePrimaryButton(
                modifier = Modifier
                    .padding(MaterialTheme.wireDimensions.spacing16x)
                    .fillMaxWidth(),
                text = stringResource(R.string.label_continue),
                onClick = onContinuePressed,
                fillMaxWidth = true,
                loading = state.loading,
                state = if (state.continueEnabled) {
                    WireButtonState.Default
                } else {
                    WireButtonState.Disabled
                },
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                BackLinkToTeamCreation(teamCreationUrl)
            }

            if (state.termsDialogVisible) {
                val context = LocalContext.current
                TermsConditionsDialog(
                    onDialogDismiss = onTermsDialogDismiss,
                    onContinuePressed = onTermsAccept,
                    onViewPolicyPressed = { CustomTabsHelper.launchUrl(context, tosUrl) }
                )
            }
            if (state.error is CreateAccountDataDetailViewState.DetailsError.DialogError.GenericError) {
                CoreFailureErrorDialog(state.error.coreFailure, onErrorDismiss)
            }
        }
    )
}

@Composable
private fun RowScope.BackLinkToTeamCreation(teamCreationUrl: String) {
    val context = LocalContext.current
    val annotatedString = buildAnnotatedString {
        append(stringResource(R.string.create_account_email_backlink_to_team_label))
        append("\n")
        withLink(
            link = LinkAnnotation.Clickable(
                tag = "teamCreation",
                styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline)),
                linkInteractionListener = { CustomTabsHelper.launchUrl(context, teamCreationUrl) }
            ),
        ) {
            append(stringResource(R.string.welcome_button_create_team))
        }
    }

    Text(
        modifier = Modifier
            .align(Alignment.CenterVertically)
            .padding(bottom = MaterialTheme.wireDimensions.spacing16x)
            .fillMaxWidth(),
        text = annotatedString,
        style = MaterialTheme.wireTypography.label04,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun RowScope.WirePrivacyPolicyLink() {
    val context = LocalContext.current
    val annotatedString = buildAnnotatedString {
        append(stringResource(R.string.create_account_email_share_anonymous_data_label))
        append(" ")
        val urlPrivacyPolicy = stringResource(R.string.url_privacy_policy)
        withLink(
            link = LinkAnnotation.Clickable(
                tag = "privacyPolicy",
                styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline)),
                linkInteractionListener = {
                    CustomTabsHelper.launchUrl(context, urlPrivacyPolicy)
                },
            ),
        ) {
            append(stringResource(R.string.create_account_email_share_anonymous_data_link_label))
        }
        append(" ")
        append(stringResource(R.string.create_account_email_share_anonymous_data_optional_label))
    }

    Text(
        modifier = Modifier.align(Alignment.CenterVertically),
        text = annotatedString,
        style = MaterialTheme.wireTypography.label04,
        textAlign = TextAlign.Start
    )
}

@Composable
private fun TermsConditionsDialog(onDialogDismiss: () -> Unit, onContinuePressed: () -> Unit, onViewPolicyPressed: () -> Unit) {
    WireDialog(
        title = stringResource(R.string.create_account_email_terms_dialog_title),
        text = stringResource(R.string.create_account_email_terms_dialog_text),
        onDismiss = onDialogDismiss
    ) {
        Column {
            WireSecondaryButton(
                text = stringResource(R.string.label_cancel),
                onClick = onDialogDismiss,
                fillMaxWidth = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.wireDimensions.spacing8x)
                    .testTag("cancelButton"),
            )
            WireSecondaryButton(
                text = stringResource(R.string.create_account_email_terms_dialog_view_policy),
                onClick = onViewPolicyPressed,
                fillMaxWidth = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.wireDimensions.spacing8x)
                    .testTag("viewTC")
            )
            WirePrimaryButton(
                text = stringResource(id = R.string.label_continue),
                onClick = onContinuePressed,
                fillMaxWidth = true,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmailErrorDetailText(error: CreateAccountDataDetailViewState.DetailsError) {
    val learnMoreTag = "learn_more"
    val context = LocalContext.current
    val learnMoreUrl = stringResource(id = R.string.url_create_account_learn_more)
    val learnMoreText = stringResource(id = R.string.label_learn_more)
    val annotatedText = buildAnnotatedString {
        append(
            if (error is CreateAccountDataDetailViewState.DetailsError.EmailFieldError) {
                when (error) {
                    CreateAccountDataDetailViewState.DetailsError.EmailFieldError.AlreadyInUseError ->
                        stringResource(R.string.create_account_email_already_in_use_error)

                    CreateAccountDataDetailViewState.DetailsError.EmailFieldError.BlacklistedEmailError ->
                        stringResource(R.string.create_account_email_blacklisted_error)

                    CreateAccountDataDetailViewState.DetailsError.EmailFieldError.DomainBlockedError ->
                        stringResource(R.string.create_account_email_domain_blocked_error)

                    CreateAccountDataDetailViewState.DetailsError.EmailFieldError.InvalidEmailError ->
                        stringResource(R.string.create_account_email_invalid_error)
                }
            } else {
                ""
            }
        )
        if (error is CreateAccountDataDetailViewState.DetailsError.EmailFieldError.AlreadyInUseError) {
            append(" ")
            pushStringAnnotation(tag = learnMoreTag, annotation = learnMoreUrl)
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.wireColorScheme.onBackground,
                    fontWeight = MaterialTheme.wireTypography.label05.fontWeight,
                    fontSize = MaterialTheme.wireTypography.label05.fontSize,
                    textDecoration = TextDecoration.Underline
                )
            ) { append(learnMoreText) }
            pop()
        }
    }
    ClickableText(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = MaterialTheme.wireDimensions.spacing8x,
                horizontal = MaterialTheme.wireDimensions.spacing16x
            ),
        style = MaterialTheme.wireTypography.label04.copy(color = MaterialTheme.wireColorScheme.error, textAlign = TextAlign.Start),
        text = annotatedText,
        onClick = { offset ->
            annotatedText.getStringAnnotations(tag = learnMoreTag, start = offset, end = offset).firstOrNull()?.let {
                CustomTabsHelper.launchUrl(context, learnMoreUrl)
            }
        }
    )
}

@Composable
@PreviewMultipleThemes
fun PreviewCreateAccountDetailsScreen() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            AccountDetailsContent(
                state = CreateAccountDataDetailViewState(),
                emailTextState = TextFieldState(),
                nameTextState = TextFieldState(),
                passwordTextState = TextFieldState(),
                confirmPasswordTextState = TextFieldState(),
                teamCreationUrl = "",
                tosUrl = "",
                onPrivacyPolicyAccepted = {},
                onTermsDialogDismiss = {},
                onTermsAccept = {},
                onBackPressed = {},
                onContinuePressed = {},
                onErrorDismiss = {},
                serverConfig = ServerConfig.DEFAULT
            )
        }
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewTosDialogsScreen() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            TermsConditionsDialog({}, {}, {})
        }
    }
}
