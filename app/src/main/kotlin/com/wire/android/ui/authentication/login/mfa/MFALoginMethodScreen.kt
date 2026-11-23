/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

package com.wire.android.ui.authentication.login.mfa

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.newauthentication.login.NewAuthContainer
import com.wire.android.ui.newauthentication.login.NewAuthHeader
import com.wire.android.ui.newauthentication.login.NewAuthTitle
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.ui.common.R as commonR

@Composable
internal fun MFALoginMethodScreen(
    methods: List<MFALoginMethod>,
    isEnabled: Boolean,
    onMethodSelected: (MFALoginMethod) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    NewAuthContainer(
        header = {
            NewAuthHeader(
                title = { NewAuthTitle(title = stringResource(R.string.mfa_login_choose_method_title)) },
                canNavigateBack = true,
                onNavigateBack = onBack,
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().testTag("mfaLoginMethodScreen")) {
            methods.forEach { method ->
                key(method.id) {
                    MFALoginMethodRow(method, isEnabled) { onMethodSelected(method) }
                }
            }
        }
    }
}

@Composable
private fun MFALoginMethodRow(method: MFALoginMethod, isEnabled: Boolean, onClick: () -> Unit) {
    val (title, description) = when (method) {
        is MFALoginMethod.Authenticator -> method.name to stringResource(R.string.mfa_default_app_name)
        MFALoginMethod.RecoveryCode -> stringResource(R.string.mfa_login_method_recovery_code) to
            stringResource(R.string.mfa_login_method_recovery_code_description)
    }
    RowItemTemplate(
        modifier = Modifier.testTag("mfaLoginMethod-${method.id}"),
        title = { Text(text = title, style = MaterialTheme.wireTypography.body02) },
        subtitle = {
            Text(
                text = description,
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
            )
        },
        actions = {
            Icon(
                painter = painterResource(commonR.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
            )
        },
        clickable = Clickable(enabled = isEnabled, onClick = onClick),
    )
}

@Composable
internal fun MFALoginRecoveryPlaceholderScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Box(modifier = Modifier.fillMaxSize().background(colorsScheme().background).testTag("mfaLoginRecoveryPlaceholder"))
}

@PreviewMultipleThemes
@Composable
private fun PreviewMFALoginMethodScreen() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            MFALoginMethodScreen(
                methods = mockMFALoginMethods,
                isEnabled = true,
                onMethodSelected = {},
                onBack = {},
            )
        }
    }
}
