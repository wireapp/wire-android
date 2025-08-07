/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.calling.common

import android.os.Build
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import com.wire.android.ui.LocalActivity

@Composable
internal fun ObservePictureInPictureMode(onChanged: (Boolean) -> Unit) {
    val activity = LocalActivity.current
    DisposableEffect(Unit) {
        val consumer = object : Consumer<PictureInPictureModeChangedInfo> {
            override fun accept(value: PictureInPictureModeChangedInfo) {
                onChanged(value.isInPictureInPictureMode)
            }
        }
        activity.addOnPictureInPictureModeChangedListener(consumer)
        onDispose {
            activity.removeOnPictureInPictureModeChangedListener(consumer)
        }
    }
}

@Composable
internal fun ObserveRotation(onChanged: (Int) -> Unit) {
    val rotation = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> LocalContext.current.display?.rotation ?: Surface.ROTATION_0
        else -> LocalActivity.current.window.windowManager.defaultDisplay.rotation
    }
    LaunchedEffect(rotation) {
        onChanged(rotation)
    }
}
