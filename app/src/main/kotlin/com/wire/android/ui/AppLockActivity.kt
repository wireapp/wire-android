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
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.ramcosta.composedestinations.generated.app.destinations.AppUnlockWithBiometricsScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.EnterLockCodeScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.SetLockCodeScreenDestination
import com.wire.android.appLogger
import com.wire.android.di.metro.LocalWireViewModelScopeKey
import com.wire.android.di.metro.createSessionViewModelGraph
import com.wire.android.di.metro.wireApplicationGraph
import com.wire.android.model.LocalWireSessionImageLoader
import com.wire.android.navigation.LoginTypeSelector
import com.wire.android.navigation.MainNavHost
import com.wire.android.navigation.rememberNavigator
import com.wire.android.session.AppUserSessionPreparationResult
import com.wire.android.session.UserSessionPreparationGate
import com.wire.android.ui.common.setupOrientationForDevice
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.SupportPage
import com.wire.android.util.SupportUrlResolver
import com.wire.android.util.launchUpdateTheApp
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class AppLockActivity : BaseActivity() {

    @Inject
    lateinit var loginTypeSelector: LoginTypeSelector

    private val qualifiedIdMapper = QualifiedIdMapper(null)
    private var preparationState by mutableStateOf<UserSessionPreparationUiState>(
        UserSessionPreparationUiState.ResolvingSession
    )
    private var preparationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        wireApplicationGraph.inject(this)
        super.onCreate(savedInstanceState)
        val userId = intent.getStringExtra(EXTRA_USER_ID)
            ?.let(qualifiedIdMapper::fromStringToQualifiedID)
            ?: run {
                appLogger.e("appLock: missing session user id, closing app lock activity")
                finish()
                return
            }
        setupOrientationForDevice()
        enableEdgeToEdge()
        showPreparation(userId)
    }

    private fun showPreparation(userId: UserId) {
        setContent {
            WireTheme {
                UserSessionPreparationScreen(
                    state = preparationState,
                    onRetry = { prepareSession(userId) },
                    onUpdate = ::updateTheApp,
                    onContactSupport = ::openSupport,
                )
            }
        }
        prepareSession(userId)
    }

    private fun prepareSession(userId: UserId) {
        if (preparationJob?.isActive == true) return
        preparationState = UserSessionPreparationUiState.ResolvingSession
        preparationJob = lifecycleScope.launch {
            val gate = UserSessionPreparationGate(wireApplicationGraph.coreLogic)
            val result = coroutineScope {
                val observer = launch(start = CoroutineStart.UNDISPATCHED) {
                    gate.observe(userId).collect { preparationState = it.toUiState() }
                }
                try {
                    gate.prepare(userId)
                } finally {
                    observer.cancelAndJoin()
                }
            }
            when (result) {
                is AppUserSessionPreparationResult.Ready -> showAppLock(userId, result.sessionScope)
                is AppUserSessionPreparationResult.Failed -> {
                    preparationState = UserSessionPreparationUiState.Failed(result.reason.toUiFailure())
                }
            }
        }
    }

    private fun showAppLock(userId: UserId, userSessionScope: UserSessionScope) {
        val sessionViewModelGraph = wireApplicationGraph.createSessionViewModelGraph(userId, userSessionScope)
        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val rememberedSessionViewModelGraph = remember { sessionViewModelGraph }
            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState,
                LocalMetroViewModelFactory provides rememberedSessionViewModelGraph.metroViewModelFactory,
                LocalWireViewModelScopeKey provides rememberedSessionViewModelGraph.viewModelScopeKey,
                LocalWireSessionImageLoader provides rememberedSessionViewModelGraph.wireSessionImageLoader,
                LocalActivity provides this
            ) {
                WireTheme {
                    val navigator = rememberNavigator(finish = this@AppLockActivity::finish)

                    val startDestination =
                        if (intent.getBooleanExtra(SET_TEAM_APP_LOCK, false)) {
                            appLogger.i("appLock: requesting set team app lock")
                            SetLockCodeScreenDestination()
                        } else {
                            val canAuthenticateWithBiometrics = BiometricManager
                                .from(this)
                                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                            if (canAuthenticateWithBiometrics == BiometricManager.BIOMETRIC_SUCCESS) {
                                appLogger.i("appLock: requesting app Unlock with biometrics")
                                AppUnlockWithBiometricsScreenDestination()
                            } else {
                                appLogger.i("appLock: requesting app Unlock with passcode")
                                EnterLockCodeScreenDestination()
                            }
                        }

                    MainNavHost(
                        navigator = navigator,
                        loginTypeSelector = loginTypeSelector,
                        startDestination = startDestination,
                    )
                }
            }
        }
    }

    private fun openSupport() {
        val supportUrl = SupportUrlResolver.resolve(resources, SupportPage.SUPPORT)
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(supportUrl)))
    }

    private fun updateTheApp() = launchUpdateTheApp()

    companion object {
        const val SET_TEAM_APP_LOCK = "set_team_app_lock"
        const val EXTRA_USER_ID = "user_id"
    }
}
