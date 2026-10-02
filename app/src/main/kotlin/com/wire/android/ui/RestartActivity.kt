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

package com.wire.android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.wire.android.ui.common.SettingUpWireScreenContent
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.theme.WireTheme

/** Runs separately so the main process and all cached user scopes can be discarded. */
class RestartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalSnackbarHostState provides remember { SnackbarHostState() }) {
                WireTheme {
                    SettingUpWireScreenContent()
                }
            }
        }
        restartMainProcess()
    }

    private fun restartMainProcess() {
        // This activity runs in :restart, so it survives terminating the original app process.
        // Discard that process and its cached user scopes so they pick up the new API version.
        val previousPid = intent.getIntExtra(EXTRA_PID, -1)
        if (previousPid > 0 && previousPid != Process.myPid()) {
            Process.killProcess(previousPid)
        }
        // Start Wire in a fresh task, then finish and terminate this temporary helper process.
        startActivity(Intent.makeRestartActivityTask(
            android.content.ComponentName(this, WireActivity::class.java)
        ))
        finish()
        Process.killProcess(Process.myPid())
    }

    companion object {
        private const val EXTRA_PID = "previous_process_id"

        fun restart(context: Context) {
            context.startActivity(Intent(context, RestartActivity::class.java).apply {
                putExtra(EXTRA_PID, Process.myPid())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
        }
    }
}
