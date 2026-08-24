/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.conversations.details.options

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType

@Composable
fun DisableConfirmationDialog(@StringRes title: Int, @StringRes text: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    WireDialog(
        title = stringResource(id = title),
        text = stringResource(id = text),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(id = R.string.label_cancel),
            type = WireDialogButtonType.Secondary,
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = onConfirm,
            text = stringResource(id = R.string.label_disable),
            type = WireDialogButtonType.Primary,
        )
    )
}
