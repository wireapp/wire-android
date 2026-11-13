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

package com.wire.android.ui.home.settings.multifactorauthentication

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.wire.android.R
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import kotlinx.coroutines.launch

internal data class AuthenticatorLinkActions(val open: () -> Unit, val copyKey: () -> Unit)

@Composable
internal fun rememberAuthenticatorLinkActions(
    enrollment: MultiFactorAuthenticationEnrollment?,
    onOpened: () -> Unit,
): AuthenticatorLinkActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbarHostState.current
    val copied = stringResource(R.string.multi_factor_authentication_copied)
    val noAuthenticator = stringResource(R.string.multi_factor_authentication_no_authenticator)
    return AuthenticatorLinkActions(
        open = {
            enrollment?.let {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, it.authenticatorUri.toUri()))
                    onOpened()
                } catch (_: ActivityNotFoundException) {
                    scope.launch { snackbar.showSnackbar(noAuthenticator) }
                }
            }
        },
        copyKey = {
            enrollment?.let {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("", it.setupKey))
                scope.launch { snackbar.showSnackbar(copied) }
            }
        },
    )
}
