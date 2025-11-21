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
package com.wire.android.ui.e2eiEnrollment
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireRootNavGraph

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.feature.NavigationSwitchAccountActions
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.LoginTypeSelector
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.authentication.devices.common.ClearSessionState
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.common.TextWithLearnMore
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.CancelLoginDialogContent
import com.wire.android.ui.common.dialogs.CancelLoginDialogState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.E2EiCertificateDetailsScreenDestination
import com.wire.android.ui.destinations.InitialSyncScreenDestination
import com.wire.android.ui.home.E2EIEnrollmentErrorWithDismissDialog
import com.wire.android.ui.home.E2EISuccessDialog
import com.wire.android.ui.settings.devices.e2ei.E2EICertificateDetails
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.Either
import com.wire.kalium.logic.feature.e2ei.usecase.E2EIEnrollmentResult

@Destination<WireRootNavGraph>(
    style = PopUpNavigationAnimation::class
)
@Composable
fun E2EIEnrollmentScreen(
    navigator: Navigator,
    loginTypeSelector: LoginTypeSelector,
    viewModel: E2EIEnrollmentViewModel = hiltViewModel(),
    clearSessionViewModel: ClearSessionViewModel = hiltViewModel(),
) {
    val state = viewModel.state

    E2EIEnrollmentScreenContent(
        state = state,
        clearSessionState = clearSessionViewModel.state,
        dismissSuccess = {
            navigator.navigate(NavigationCommand(InitialSyncScreenDestination, BackStackMode.CLEAR_WHOLE))
            viewModel.finalizeMLSClient()
        },
        dismissErrorDialog = viewModel::dismissErrorDialog,
        enrollE2EICertificate = viewModel::enrollE2EICertificate,
        handleE2EIEnrollmentResult = viewModel::handleE2EIEnrollmentResult,
        openCertificateDetails = {
            navigator.navigate(
                NavigationCommand(
                    E2EiCertificateDetailsScreenDestination(
                        E2EICertificateDetails.DuringLoginCertificateDetails(state.certificate)
                    )
                )
            )
        },
        onBackButtonClicked = clearSessionViewModel::onBackButtonClicked,
        onCancelEnrollmentClicked = {
            clearSessionViewModel.onCancelLoginClicked(
                NavigationSwitchAccountActions(navigator::navigate, loginTypeSelector::canUseNewLogin)
            )
        },
        onProceedEnrollmentClicked = clearSessionViewModel::onProceedLoginClicked,
    )
}

@Composable
private fun E2EIEnrollmentScreenContent(
    state: E2EIEnrollmentState,
    clearSessionState: ClearSessionState,
    dismissSuccess: () -> Unit,
    dismissErrorDialog: () -> Unit,
    enrollE2EICertificate: () -> Unit,
    handleE2EIEnrollmentResult: (Either<CoreFailure, E2EIEnrollmentResult>) -> Unit,
    openCertificateDetails: () -> Unit,
    onBackButtonClicked: () -> Unit,
    onCancelEnrollmentClicked: () -> Unit,
    onProceedEnrollmentClicked: () -> Unit
) {
    BackHandler {
        onBackButtonClicked()
    }
    val cancelLoginDialogState = rememberVisibilityState<CancelLoginDialogState>()
    CancelLoginDialogContent(
        dialogState = cancelLoginDialogState,
        onActionButtonClicked = onCancelEnrollmentClicked,
        onProceedButtonClicked = onProceedEnrollmentClicked,
    )
    if (clearSessionState.showCancelLoginDialog) {
        cancelLoginDialogState.show(
            cancelLoginDialogState.savedState ?: CancelLoginDialogState
        )
    } else {
        cancelLoginDialogState.dismiss()
    }
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = dimensions().spacing0x,
                title = stringResource(id = R.string.end_to_end_identity_required_dialog_title),
                navigationIconType = NavigationIconType.Close(),
                onNavigationPressed = onBackButtonClicked
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) {
                WirePrimaryButton(
                    onClick = enrollE2EICertificate,
                    text = stringResource(id = R.string.end_to_end_identity_required_dialog_positive_button),
                    state = WireButtonState.Default,
                    loading = state.isLoading,
                    modifier = Modifier.padding(
                        top = dimensions().spacing16x,
                        start = dimensions().spacing16x,
                        end = dimensions().spacing16x,
                        bottom = dimensions().spacing16x
                    )
                )
            }
        }
    ) { internalPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .padding(internalPadding)
                .padding(MaterialTheme.wireDimensions.spacing16x)
        ) {
            val text = buildAnnotatedString {
                val style = SpanStyle(
                    color = colorsScheme().onBackground,
                    fontWeight = MaterialTheme.wireTypography.body01.fontWeight,
                    fontSize = MaterialTheme.wireTypography.body01.fontSize,
                    fontFamily = MaterialTheme.wireTypography.body01.fontFamily,
                    fontStyle = MaterialTheme.wireTypography.body01.fontStyle
                )
                withStyle(style) { append(stringResource(id = R.string.end_to_end_identity_required_dialog_text_no_snooze)) }
            }
            TextWithLearnMore(
                textAnnotatedString = text,
                learnMoreLink = stringResource(R.string.url_e2ee_id_shield),
                modifier = Modifier.padding(
                    top = MaterialTheme.wireDimensions.dialogTextsSpacing,
                    bottom = MaterialTheme.wireDimensions.dialogTextsSpacing,
                ),
            )
        }

        if (state.isCertificateEnrollError) {
            E2EIEnrollmentErrorWithDismissDialog(
                isE2EILoading = state.isLoading,
                onClick = enrollE2EICertificate,
                onDismiss = dismissErrorDialog
            )
        }

        if (state.isCertificateEnrollSuccess) {
            E2EISuccessDialog(
                openCertificateDetails = openCertificateDetails,
                dismissDialog = dismissSuccess
            )
        }

        if (state.startGettingE2EICertificate) {
            GetE2EICertificateUI(
                enrollmentResultHandler = { handleE2EIEnrollmentResult(it) },
                isNewClient = true
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewE2EIEnrollmentScreenContent() {
    WireTheme {
        E2EIEnrollmentScreenContent(
            E2EIEnrollmentState(), ClearSessionState(), {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewE2EIEnrollmentScreenContentWithSuccess() {
    WireTheme {
        E2EIEnrollmentScreenContent(
            E2EIEnrollmentState(isCertificateEnrollSuccess = true), ClearSessionState(), {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewE2EIEnrollmentScreenContentWithError() {
    WireTheme {
        E2EIEnrollmentScreenContent(
            E2EIEnrollmentState(isCertificateEnrollError = true), ClearSessionState(), {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}
