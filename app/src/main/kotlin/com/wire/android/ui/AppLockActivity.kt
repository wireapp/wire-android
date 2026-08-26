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

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.wire.android.appLogger
import com.wire.android.di.metro.WireApplicationGraph
import com.wire.android.di.metro.wireApplicationGraph
import com.wire.android.navigation.navigation3.WireNav3Host
import com.wire.android.navigation.navigation3.rememberWireNavigation3Runtime
import com.wire.android.navigation.runtime.MetroWireEntryEnvironment
import com.wire.android.navigation.runtime.SessionGraphStoreViewModel
import com.wire.android.navigation.runtime.wireSessionGraphStoreViewModel
import com.wire.android.ui.common.setupOrientationForDevice
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.home.appLock.AppLockNavigation3Actions
import com.wire.android.ui.home.appLock.AppLockNavigation3Contribution
import com.wire.android.ui.home.appLock.AppUnlockWithBiometricsRoute
import com.wire.android.ui.home.appLock.EnterLockCodeRoute
import com.wire.android.ui.home.appLock.SetLockCodeRoute
import com.wire.android.ui.home.appLock.resolveAppLockStartRoute
import com.wire.android.ui.theme.WireTheme
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireSessionId

class AppLockActivity : BaseActivity() {

    private val qualifiedIdMapper = QualifiedIdMapper(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionId = intent.getStringExtra(EXTRA_USER_ID)
            ?.let(qualifiedIdMapper::fromStringToQualifiedID)
            ?.let { WireSessionId(it.value, it.domain) }
            ?: run {
                appLogger.e("appLock: missing session user id, closing app lock activity")
                finish()
                return
            }
        val setTeamAppLock = intent.getBooleanExtra(SET_TEAM_APP_LOCK, false)
        val canAuthenticateWithBiometrics = BiometricManager
            .from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
        val startRoute = resolveAppLockStartRoute(
            sessionId = sessionId,
            setTeamAppLock = setTeamAppLock,
            canAuthenticateWithBiometrics = canAuthenticateWithBiometrics,
        )
        when (startRoute) {
            is SetLockCodeRoute -> appLogger.i("appLock: requesting set team app lock")
            is AppUnlockWithBiometricsRoute ->
                appLogger.i("appLock: requesting app Unlock with biometrics")
            is EnterLockCodeRoute -> appLogger.i("appLock: requesting app Unlock with passcode")
        }

        setupOrientationForDevice()
        enableEdgeToEdge()
        setContent {
            AppLockNavigation3Root(
                appGraph = this@AppLockActivity.wireApplicationGraph,
                startRoute = startRoute,
            )
        }
    }

    @Composable
    private fun AppLockNavigation3Root(
        appGraph: WireApplicationGraph,
        startRoute: SessionRoute,
        sessionGraphStore: SessionGraphStoreViewModel = wireSessionGraphStoreViewModel(
            appGraph = appGraph,
            owner = this@AppLockActivity,
        ),
    ) {
        val snackbarHostState = remember { SnackbarHostState() }
        val runtime = rememberWireNavigation3Runtime(startRoute)
        val actions = remember {
            object : AppLockNavigation3Actions {
                override fun completeUnlock() = finish()

                override fun cancelUnlock() = finishAffinity()

                override fun restartAfterLogout() = this@AppLockActivity.restartAfterLogout()
            }
        }
        val entryEnvironment = remember(appGraph, sessionGraphStore) {
            MetroWireEntryEnvironment(
                appGraph = appGraph,
                authenticationGraph = appGraph.authenticationViewModelGraph,
                sessionGraphStore = sessionGraphStore,
                logoutAction = { this@AppLockActivity.restartAfterLogout() },
            )
        }
        val entryProviderInstallers = remember(runtime, actions) {
            AppLockNavigation3Contribution.entryProviderInstallers(runtime, actions)
        }

        CompositionLocalProvider(
            LocalSnackbarHostState provides snackbarHostState,
            LocalActivity provides this,
        ) {
            WireTheme {
                WireNav3Host(
                    runtime = runtime,
                    entryEnvironment = entryEnvironment,
                    entryProviderInstallers = entryProviderInstallers,
                )
            }
        }
    }

    private fun restartAfterLogout() {
        startActivity(
            Intent(this, WireActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    companion object {
        const val SET_TEAM_APP_LOCK = "set_team_app_lock"
        const val EXTRA_USER_ID = "user_id"
    }
}
