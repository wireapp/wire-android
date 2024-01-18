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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.ClickableText
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.wirePrimaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.destinations.E2eiCertificateDetailsScreenDestination
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.InitialSyncScreenDestination
import com.wire.android.ui.home.E2EIErrorWithDismissDialog
import com.wire.android.ui.home.E2EISuccessDialog
import com.wire.android.ui.markdown.MarkdownConstants
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@RootNavGraph
@Destination(navArgsDelegate = E2EIEnrollmentNavArgs::class)
@Composable
fun E2EIEnrollmentScreen(
    navigator: Navigator,
    viewModel: E2EIEnrollmentViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current

    E2EIEnrollmentScreenContent(state = state,
        dismissSuccess = {
            viewModel.finishUp()
            navigator.navigate(NavigationCommand(InitialSyncScreenDestination, BackStackMode.CLEAR_WHOLE))
        },
        dismissErrorDialog = viewModel::dismissErrorDialog,
        enrollE2EICertificate = { viewModel.enrollE2EICertificate(context) },
        openCertificateDetails = {
            viewModel.finishUp()
            navigator.navigate(NavigationCommand(E2eiCertificateDetailsScreenDestination(state.certificate)))
        }
    )
}

@Composable
private fun E2EIEnrollmentScreenContent(
    state: E2EIEnrollmentState,
    dismissSuccess: () -> Unit,
    dismissErrorDialog: () -> Unit,
    enrollE2EICertificate: () -> Unit,
    openCertificateDetails: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    WireScaffold(
        bottomBar = {
            Column(
                Modifier
                    .background(MaterialTheme.wireColorScheme.surface)
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(PaddingValues(MaterialTheme.wireDimensions.dialogContentPadding)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.end_to_end_identity_required_dialog_title),
                style = MaterialTheme.wireTypography.title02,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(dimensions().spacing16x))
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
            ClickableText(
                text = text,
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier.padding(
                    top = MaterialTheme.wireDimensions.dialogTextsSpacing,
                    bottom = MaterialTheme.wireDimensions.dialogTextsSpacing,
                ),
                onClick = { offset ->
                    text.getStringAnnotations(
                        tag = MarkdownConstants.TAG_URL,
                        start = offset,
                        end = offset,
                    ).firstOrNull()?.let { result -> uriHandler.openUri(result.item) }
                }
            )
        }

        if (state.isCertificateEnrollError) {
            E2EIErrorWithDismissDialog(
                isE2EILoading = state.isLoading,
                updateCertificate = enrollE2EICertificate,
                onDismiss = dismissErrorDialog
            )
        }

        if (state.isCertificateEnrollSuccess) {
            E2EISuccessDialog(
                openCertificateDetails = openCertificateDetails,
                dismissDialog = dismissSuccess
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun previewE2EIEnrollmentScreenContent() {
    WireTheme {
        E2EIEnrollmentScreenContent(E2EIEnrollmentState(), {}, {}, {}) { }
    }
}

@PreviewMultipleThemes
@Composable
fun previewE2EIEnrollmentScreenContentWithSuccess() {
    WireTheme {
        E2EIEnrollmentScreenContent(E2EIEnrollmentState(isCertificateEnrollSuccess = true), {}, {}, {}) { }
    }
}

@PreviewMultipleThemes
@Composable
fun previewE2EIEnrollmentScreenContentWithError() {
    WireTheme {
        E2EIEnrollmentScreenContent(E2EIEnrollmentState(isCertificateEnrollError = true), {}, {}, {}) { }
    }
}


