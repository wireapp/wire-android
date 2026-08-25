/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration.details

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.theme.wireDimensions

@Composable
internal fun <FailureT> LegacyRegistrationDetailsActions(
    state: LegacyRegistrationDetailsState<FailureT>,
    continueLabel: String,
    onContinuePressed: () -> Unit,
    footer: @Composable () -> Unit,
    dialogs: @Composable () -> Unit,
) {
    WirePrimaryButton(
        text = continueLabel,
        onClick = onContinuePressed,
        loading = state.loading,
        fillMaxWidth = true,
        state = if (state.continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.wireDimensions.spacing16x),
    )
    footer()
    dialogs()
}
