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

package com.wire.android.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenStateObserver @Inject constructor(@ApplicationContext val context: Context) : BroadcastReceiver() {

    private val _screenStateFlow = MutableStateFlow(true)
    val screenStateFlow = _screenStateFlow.asStateFlow()

    init {
        val pm: PowerManager? = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
        _screenStateFlow.value = pm?.isInteractive ?: true

        context.registerReceiver(
            this,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
        )
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        if (p1?.action == Intent.ACTION_SCREEN_OFF) {
            _screenStateFlow.value = false
        }
        if (p1?.action == Intent.ACTION_SCREEN_ON) {
            _screenStateFlow.value = true
        }
    }

    companion object {
        const val TAG = "ScreenStateObserver"
    }
}
