/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.e2eiEnrollment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions

data class E2EIEnrollmentText(
    val title: String,
    val message: String,
    val enrollLabel: String,
    val learnMoreUrl: String,
)

/**
 * Presentation owned by authentication. The host supplies OAuth, account-cancellation and shared
 * dialogs because those have app/session dependencies.
 */
@Composable
fun E2EIEnrollmentContent(
    state: E2EIEnrollmentState,
    text: E2EIEnrollmentText,
    onBackButtonClicked: () -> Unit,
    onEnroll: () -> Unit,
    cancelDialog: @Composable () -> Unit,
    enrollmentRequest: @Composable ((E2EIEnrollmentResult) -> Unit) -> Unit,
    errorDialog: @Composable (onRetry: () -> Unit, onDismiss: () -> Unit) -> Unit,
    successDialog: @Composable (certificate: String, onOpenDetails: () -> Unit, onDismiss: () -> Unit, isFinalizing: Boolean) -> Unit,
    learnMoreContent: @Composable (message: String, url: String) -> Unit,
    onEnrollmentResult: (E2EIEnrollmentResult) -> Unit,
    onDismissError: () -> Unit,
    onOpenCertificateDetails: (String) -> Unit,
    onDismissSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackButtonClicked)
    cancelDialog()
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = dimensions().spacing0x,
                title = text.title,
                navigationIconType = NavigationIconType.Close(),
                onNavigationPressed = onBackButtonClicked,
            )
        },
        bottomBar = {
            Column(Modifier.wrapContentWidth(Alignment.CenterHorizontally)) {
                WirePrimaryButton(
                    onClick = onEnroll,
                    text = text.enrollLabel,
                    state = WireButtonState.Default,
                    loading = state.isLoading,
                    modifier = Modifier.padding(
                        top = dimensions().spacing16x,
                        start = dimensions().spacing16x,
                        end = dimensions().spacing16x,
                        bottom = dimensions().spacing16x,
                    ),
                )
            }
        },
    ) { internalPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .padding(internalPadding)
                .padding(MaterialTheme.wireDimensions.spacing16x),
        ) {
            learnMoreContent(
                text.message,
                text.learnMoreUrl,
            )
        }
        if (state.isCertificateEnrollError) errorDialog(onEnroll, onDismissError)
        if (state.isCertificateEnrollSuccess) {
            successDialog(
                state.certificate,
                { onOpenCertificateDetails(state.certificate) },
                onDismissSuccess,
                state.isFinalizing,
            )
        }
        if (state.startGettingE2EICertificate) enrollmentRequest(onEnrollmentResult)
    }
}
