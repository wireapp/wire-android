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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class RecoveryCodeActions(val copy: () -> Unit, val download: () -> Unit)

@Composable
internal fun rememberRecoveryCodeActions(codes: List<String>): RecoveryCodeActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbarHostState.current
    val copied = stringResource(R.string.multi_factor_authentication_copied)
    val saved = stringResource(R.string.multi_factor_authentication_codes_downloaded)
    val saveError = stringResource(R.string.multi_factor_authentication_codes_download_error)
    val download = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null && codes.isNotEmpty()) {
            scope.launch {
                val success = writeRecoveryCodes(context, uri, codes)
                snackbar.showSnackbar(if (success) saved else saveError)
            }
        }
    }
    return RecoveryCodeActions(
        copy = {
            if (codes.isNotEmpty()) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("", codes.joinToString("\n")))
                scope.launch { snackbar.showSnackbar(copied) }
            }
        },
        download = { if (codes.isNotEmpty()) download.launch("wire-recovery-codes.txt") },
    )
}

private suspend fun writeRecoveryCodes(context: Context, uri: Uri, codes: List<String>): Boolean = withContext(Dispatchers.IO) {
    try {
        val stream = context.contentResolver.openOutputStream(uri) ?: return@withContext false
        stream.bufferedWriter().use { it.write(codes.joinToString("\n")) }
        true
    } catch (_: IOException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
