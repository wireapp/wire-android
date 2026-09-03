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
package com.wire.android.ui.home.appLock.unlock

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.biometric.showBiometricPrompt
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions

@Composable
internal fun AppUnlockWithBiometricsRouteScreen(
    onUnlocked: () -> Unit,
    onCancel: () -> Unit,
    onRequestPasscode: () -> Unit,
) {
    AppUnLockBackground()

    val activity = LocalActivity.current as AppCompatActivity
    val tooManyAttemptsMessage = stringResource(
        id = R.string.biometrics_app_unlock_too_many_attempts
    )

    LaunchedEffect(Unit) {
        activity.showBiometricPrompt(
            onSuccess = {
                appLogger.i("appLock: app Unlocked with biometrics")
                onUnlocked()
            },
            onCancel = {
                appLogger.i("appLock: biometrics unlock canceled")
                onCancel()
            },
            onTooManyFailedAttempts = {
                Toast.makeText(activity, tooManyAttemptsMessage, Toast.LENGTH_SHORT).show()
                onRequestPasscode()
            },
            onRequestPasscode = {
                appLogger.i("appLock: requesting passcode from biometrics unlock")
                onRequestPasscode()
            }
        )
    }
}

@Composable
private fun AppUnLockBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorsScheme().background)
    ) {
        Icon(
            modifier = Modifier
                .padding(top = dimensions().spacing80x)
                .align(Alignment.TopCenter),
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_wire_logo),
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = stringResource(id = R.string.content_description_welcome_wire_logo)
        )
    }
}
