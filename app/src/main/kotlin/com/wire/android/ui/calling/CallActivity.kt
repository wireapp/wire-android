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
package com.wire.android.ui.calling

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wire.android.appLogger
import com.wire.android.di.metro.WireViewModelDiagnostics
import com.wire.android.di.metro.wireApplicationGraph
import com.wire.android.model.LocalWireSessionImageLoader
import com.wire.android.navigation.navigation3.rememberWireViewModelStoreProvider
import com.wire.android.navigation.navigation3.rememberWireSharedViewModelStoreOwner
import com.wire.android.navigation.navigation3.clearWireViewModelStoreOwner
import com.wire.android.navigation.runtime.wireSessionGraphStoreViewModel
import com.wire.android.ui.AppLockActivity
import com.wire.android.ui.BaseActivity
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.calling.common.ProximitySensorManager
import com.wire.android.ui.common.commonTopAppBarViewModel
import com.wire.android.ui.common.setupOrientationForDevice
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.CommonTopAppBarParams
import com.wire.android.ui.common.topappbar.WireTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.SwitchAccountObserver
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.navigation.WireSessionId
import com.wire.navigation.WireViewModelOwner
import com.wire.navigation.stableKey
import dev.zacsweers.metro.HasMemberInjections
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provider
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

@HasMemberInjections
abstract class CallActivity : BaseActivity() {

    @Inject
    lateinit var switchAccountObserver: SwitchAccountObserver

    @Inject
    lateinit var proximitySensorManager: ProximitySensorManager

    @Inject
    lateinit var callActivityViewModelProvider: Provider<CallActivityViewModel>

    private val callActivityViewModel: CallActivityViewModel by viewModels {
        viewModelFactory {
            initializer { callActivityViewModelProvider() }
        }
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_SCREEN_TYPE = "screen_type"
        const val EXTRA_SHOULD_ANSWER_CALL = "should_answer_call"
        const val TAG = "CallActivity"
    }

    private var request: CallActivityRequest? by mutableStateOf(null)
    private val qualifiedIdMapper = QualifiedIdMapper(null)

    protected abstract val destination: CallActivityDestination

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        accept(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        wireApplicationGraph.inject(this)
        super.onCreate(savedInstanceState)

        if (!accept(intent)) {
            return
        }

        setupOrientationForDevice()
        setUpCallingFlags()

        enableEdgeToEdge()

        appLogger.i("$TAG Initializing proximity sensor..")
        proximitySensorManager.initialize()

        setContent {
            request?.let { CallActivityRoot(it) }
        }
    }

    @Composable
    protected abstract fun Content(request: CallActivityRequest)

    private fun accept(intent: Intent): Boolean {
        val parsedRequest = intent.toCallActivityRequest(destination, qualifiedIdMapper)
        if (parsedRequest == null) {
            appLogger.e("$TAG invalid call intent for $destination, closing call activity")
            finish()
            return false
        }
        setIntent(intent)
        request = parsedRequest
        return true
    }

    @Composable
    private fun CallActivityRoot(request: CallActivityRequest) {
        val sessionId = WireSessionId(request.userId.value, request.userId.domain)
        val ownerIdentity = WireViewModelOwner.Session(sessionId)
        val ownerKey = ownerIdentity.stableKey()
        val storeProvider = rememberWireViewModelStoreProvider(this)
        val sessionGraphStore = wireSessionGraphStoreViewModel(
            appGraph = wireApplicationGraph,
            owner = this,
        )
        key(ownerKey) {
            val sessionGraphResult = remember(sessionId) {
                runCatching { sessionGraphStore.graphFor(request.userId) }
            }
            val sessionGraph = sessionGraphResult.getOrNull()
            if (sessionGraph == null) {
                LaunchedEffect(sessionId) {
                    appLogger.e(
                        "$TAG unavailable call session, closing call activity",
                        sessionGraphResult.exceptionOrNull(),
                    )
                    finish()
                }
            } else {
                val sessionOwner = rememberWireSharedViewModelStoreOwner(
                    key = ownerKey,
                    provider = storeProvider,
                )
                DisposableEffect(ownerIdentity, sessionOwner) {
                    WireViewModelDiagnostics.ownerAvailable(sessionOwner, ownerKey)
                    onDispose {
                        WireViewModelDiagnostics.ownerReleased(sessionOwner, ownerKey)
                        if (!isChangingConfigurations) {
                            clearWireViewModelStoreOwner(storeProvider, ownerKey) {
                                WireViewModelDiagnostics.ownerCleared(ownerKey)
                            }
                            sessionGraphStore.release(request.userId)
                        }
                    }
                }

                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides sessionOwner,
                    LocalMetroViewModelFactory provides sessionGraph.metroViewModelFactory,
                    LocalWireSessionImageLoader provides sessionGraph.wireSessionImageLoader,
                    LocalActivity provides this,
                    LocalSnackbarHostState provides remember { SnackbarHostState() },
                ) {
                    LaunchedEffect(callActivityViewModel, request.userId) {
                        setScreenshotPreventionFlag(
                            callActivityViewModel.isScreenshotCensoringConfigEnabled(request.userId).await()
                        )
                        callActivityViewModel.switchAccountIfNeeded(request.userId, switchAccountObserver)
                    }
                    val commonTopAppBarViewModel = commonTopAppBarViewModel(
                        params = CommonTopAppBarParams(
                            showNoNetwork = true,
                            showSync = false,
                            showActiveCalls = false,
                        ),
                        owner = sessionOwner,
                    )
                    WireTheme {
                        Column(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
                            WireTopAppBar(
                                commonTopAppBarState = commonTopAppBarViewModel.state,
                                animateContentSize = false,
                            )
                            Box(modifier = Modifier.consumeWindowInsets(WindowInsets.statusBars)) {
                                Content(request)
                            }
                        }
                    }
                }
            }
        }
    }

    fun openAppLockActivity() {
        Intent(this, AppLockActivity::class.java)
            .apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                intent.getStringExtra(EXTRA_USER_ID)?.let {
                    putExtra(AppLockActivity.EXTRA_USER_ID, it)
                }
            }.run {
                startActivity(this)
            }
    }

    fun setUpCallingFlags() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    fun cleanUpCallingFlags() {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false)
            setTurnScreenOn(false)
        } else {
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun setScreenshotPreventionFlag(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
