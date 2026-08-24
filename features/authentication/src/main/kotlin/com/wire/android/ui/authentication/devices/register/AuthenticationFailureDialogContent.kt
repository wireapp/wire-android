/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.authentication.devices.register

import androidx.compose.runtime.Composable
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType

@Composable
fun AuthenticationFailureDialogContent(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
) {
    WireDialog(
        title = title,
        text = message,
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = confirmLabel,
            type = WireDialogButtonType.Primary,
        ),
    )
}
