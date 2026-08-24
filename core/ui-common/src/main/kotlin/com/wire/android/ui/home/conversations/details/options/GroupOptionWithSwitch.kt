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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.home.settings.SwitchState
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun GroupOptionWithSwitch(
    switchState: Boolean,
    switchClickable: Boolean,
    switchVisible: Boolean,
    isLoading: Boolean,
    onClick: (Boolean) -> Unit,
    @StringRes title: Int,
    @StringRes subTitle: Int?,
) {
    GroupConversationOptionsItem(
        title = stringResource(id = title),
        subtitle = subTitle?.let { stringResource(id = it) },
        switchState = when {
            !switchVisible -> SwitchState.TextOnly(value = switchState)
            switchClickable && !isLoading -> SwitchState.Enabled(value = switchState, onCheckedChange = onClick)
            else -> SwitchState.Disabled(value = switchState)
        },
        arrowType = ArrowType.NONE
    )
    HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.wireColorScheme.divider)
}
