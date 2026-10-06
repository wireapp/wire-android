/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.wire.android.ui.common.dialogs.StartupAccountLimitDialog
import com.wire.android.ui.theme.WireTheme

@Preview(name = "Limit one", showBackground = true, widthDp = 360, heightDp = 800)
@PreviewTest
@Composable
fun StartupAccountLimitOnePreview() {
    WireTheme {
        StartupAccountLimitDialog(onLogout = {}, onDismiss = {}, maxAccounts = 1)
    }
}

@Preview(name = "Limit three", showBackground = true, widthDp = 360, heightDp = 800)
@PreviewTest
@Composable
fun StartupAccountLimitThreePreview() {
    WireTheme {
        StartupAccountLimitDialog(onLogout = {}, onDismiss = {}, maxAccounts = 3)
    }
}
