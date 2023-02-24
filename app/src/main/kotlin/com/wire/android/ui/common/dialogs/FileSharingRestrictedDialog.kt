package com.wire.android.ui.common.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.visbility.VisibilityState

@Composable
fun FileSharingRestrictedDialogContent(
    dialogState: VisibilityState<FileSharingRestrictedDialogState>
) {
    VisibilityState(dialogState) {
        WireDialog(
            title = stringResource(R.string.file_sharing_restricted_dialog_title),
            text = stringResource(R.string.file_sharing_restricted_dialog_description),
            onDismiss = dialogState::dismiss,
            optionButton1Properties =
            WireDialogButtonProperties(
                onClick = dialogState::dismiss,
                text = stringResource(R.string.label_ok),
                type = WireDialogButtonType.Primary
            )
        )
    }
}
