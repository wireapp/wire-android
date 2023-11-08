/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import com.wire.android.appLogger
import com.wire.android.navigation.NavigationGraph
import com.wire.android.navigation.rememberNavigator
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.destinations.AppUnlockWithBiometricsScreenDestination
import com.wire.android.ui.destinations.EnterLockCodeScreenDestination
import com.wire.android.ui.destinations.SetLockCodeScreenDestination
import com.wire.android.ui.theme.WireTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppLockActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState,
                LocalActivity provides this
            ) {
                WireTheme {
                    val navigator = rememberNavigator(this@AppLockActivity::finish)

                val startDestination =
                    if (intent.getBooleanExtra(SET_TEAM_APP_LOCK, false)) {
                        appLogger.i("appLock: requesting set team app lock")
                        SetLockCodeScreenDestination
                    } else {
                        val canAuthenticateWithBiometrics = BiometricManager
                            .from(this)
                            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        if (canAuthenticateWithBiometrics == BiometricManager.BIOMETRIC_SUCCESS) {
                            appLogger.i("appLock: requesting app Unlock with biometrics")
                            AppUnlockWithBiometricsScreenDestination
                        } else {
                            appLogger.i("appLock: requesting app Unlock with passcode")
                            EnterLockCodeScreenDestination
                        }
                    }

                NavigationGraph(
                    navigator = navigator,
                    startDestination = startDestination
                )
            }
        }
    }

    companion object {
        const val SET_TEAM_APP_LOCK = "set_team_app_lock"
    }
}

    companion object {
        const val SET_TEAM_APP_LOCK = "set_team_app_lock"
    }
}
