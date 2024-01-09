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

@file:Suppress("TooManyFunctions")

package com.wire.android.ui.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun WelcomeNewUserDialog(
    dismissDialog: () -> Unit,
    context: Context = LocalContext.current
) {
    val welcomeToNewAndroidUrl = stringResource(id = R.string.url_welcome_to_new_android)
    WireDialog(
        title = stringResource(id = R.string.welcome_migration_dialog_title),
        text = stringResource(id = R.string.welcome_migration_dialog_content),
        onDismiss = dismissDialog,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = {
                dismissDialog.invoke()
                CustomTabsHelper.launchUrl(context, welcomeToNewAndroidUrl)
            },
            text = stringResource(id = R.string.label_learn_more),
            type = WireDialogButtonType.Primary,
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = dismissDialog,
            text = stringResource(id = R.string.welcome_migration_dialog_continue),
            type = WireDialogButtonType.Primary,
        )
    )
}

@PreviewMultipleThemes
@Composable
fun previewWelcomeNewUserDialog() {
    WireTheme {
        WelcomeNewUserDialog({})
    }
}
