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

@file:Suppress("StringTemplate")

package com.wire.android.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.wire.android.appLogger

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LifecycleLogger @Inject constructor() : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        appLogger.i("$TAG app onCreate")
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        appLogger.i("$TAG app onStart")
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        appLogger.i("$TAG app onResume")
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        appLogger.i("$TAG app onPause")
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        appLogger.i("$TAG app onStop")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        appLogger.i("$TAG app onDestroy")
    }

    private companion object {
        const val TAG = "LifecycleLogger"
    }
}
