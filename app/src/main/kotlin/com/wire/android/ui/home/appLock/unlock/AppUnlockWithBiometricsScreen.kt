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

import com.wire.android.navigation.annotation.app.WireRootDestination
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.biometric.showBiometricPrompt
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.ramcosta.composedestinations.generated.app.destinations.EnterLockCodeScreenDestination

@WireRootDestination
@Composable
fun AppUnlockWithBiometricsScreen(
    navigator: Navigator,
    appUnlockWithBiometricsViewModel: AppUnlockWithBiometricsViewModel = hiltViewModel()
) {
    AppUnLockBackground()

    val context = LocalContext.current
    val tooManyAttemptsMessage = stringResource(
        id = R.string.biometrics_app_unlock_too_many_attempts
    )

    LaunchedEffect(Unit) {
        (context as AppCompatActivity).showBiometricPrompt(
            onSuccess = {
                appLogger.i("appLock: app Unlocked with biometrics")
                appUnlockWithBiometricsViewModel.onAppUnlocked()
                navigator.navigateBack()
            },
            onCancel = {
                appLogger.i("appLock: biometrics unlock canceled")
                context.finishAffinity()
            },
            onTooManyFailedAttempts = {
                Toast.makeText(context, tooManyAttemptsMessage, Toast.LENGTH_SHORT).show()
                navigator.navigate(
                    NavigationCommand(
                        destination = EnterLockCodeScreenDestination,
                        backStackMode = BackStackMode.REMOVE_CURRENT
                    )
                )
            },
            onRequestPasscode = {
                appLogger.i("appLock: requesting passcode from biometrics unlock")
                navigator.navigate(
                    NavigationCommand(
                        destination = EnterLockCodeScreenDestination,
                        backStackMode = BackStackMode.REMOVE_CURRENT
                    )
                )
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
