package com.wire.android.ui.userprofile.self.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.WireLabelledCheckbox
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.visbility.VisibilityState

@Composable
fun LogoutOptionsDialog(
    dialogState: VisibilityState<LogoutOptionsDialogState>,
    logout: (Boolean) -> Unit
) {
    VisibilityState(dialogState) { state ->
        WireDialog(
            title = stringResource(R.string.dialog_logout_wipe_data_title),
            buttonsHorizontalAlignment = true,
            onDismiss = dialogState::dismiss,
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = dialogState::dismiss,
                text = stringResource(id = R.string.label_cancel),
                state = WireButtonState.Default
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = remember(state) { { logout(state.shouldWipeData).also { dialogState.dismiss() } } },
                text = stringResource(id = R.string.user_profile_logout),
                type = WireDialogButtonType.Primary,
                state = WireButtonState.Default
            )
        ) {
            WireLabelledCheckbox(
                label = stringResource(R.string.dialog_logout_wipe_data_checkbox),
                checked = state.shouldWipeData,
                onCheckClicked = remember { { dialogState.show(state.copy(shouldWipeData = it)) } },
                horizontalArrangement = Arrangement.Center,
                contentPadding = PaddingValues(vertical = dimensions().spacing4x),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensions().spacing16x)
                    .clip(RoundedCornerShape(size = dimensions().spacing4x))
            )
        }
    }
}
