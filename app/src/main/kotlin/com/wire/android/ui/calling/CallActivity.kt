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
import android.net.Uri
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wire.android.appLogger
import com.wire.android.di.metro.LocalWireViewModelScopeKey
import com.wire.android.di.metro.createSessionViewModelGraph
import com.wire.android.di.metro.wireApplicationGraph
import com.wire.android.model.LocalWireSessionImageLoader
import com.wire.android.ui.AppLockActivity
import com.wire.android.ui.BaseActivity
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.UserSessionPreparationScreen
import com.wire.android.ui.UserSessionPreparationUiState
import com.wire.android.ui.toUiFailure
import com.wire.android.ui.toUiState
import com.wire.android.ui.calling.common.ProximitySensorManager
import com.wire.android.ui.common.setupOrientationForDevice
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.CommonTopAppBarParams
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import com.wire.android.ui.common.topappbar.WireTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.session.AppUserSessionPreparationResult
import com.wire.android.session.UserSessionPreparationGate
import com.wire.android.util.SupportPage
import com.wire.android.util.SupportUrlResolver
import com.wire.android.util.SwitchAccountObserver
import com.wire.android.util.launchUpdateTheApp
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import dev.zacsweers.metro.HasMemberInjections
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provider
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@HasMemberInjections
@Suppress("TooManyFunctions")
abstract class CallActivity : BaseActivity() {

    @Inject
    lateinit var switchAccountObserver: SwitchAccountObserver

    @Inject
    lateinit var proximitySensorManager: ProximitySensorManager

    @Inject
    lateinit var callActivityViewModelProvider: Provider<CallActivityViewModel>

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_SCREEN_TYPE = "screen_type"
        const val EXTRA_SHOULD_ANSWER_CALL = "should_answer_call"
        const val TAG = "CallActivity"
    }

    private val imageAssetViewModelGraph by lazy { wireApplicationGraph.imageAssetViewModelGraph }
    private val commonTopAppBarViewModel: CommonTopAppBarViewModel by viewModels {
        viewModelFactory {
            initializer {
                imageAssetViewModelGraph.commonTopAppBarViewModelFactory.create(
                    CommonTopAppBarParams(showNoNetwork = true, showSync = false, showActiveCalls = false)
                )
            }
        }
    }
    private val callActivityViewModel: CallActivityViewModel by viewModels {
        viewModelFactory {
            initializer {
                callActivityViewModelProvider()
            }
        }
    }
    protected val qualifiedIdMapper = QualifiedIdMapper(null)
    private var preparationState by mutableStateOf<UserSessionPreparationUiState>(
        UserSessionPreparationUiState.ResolvingSession
    )
    private var preparationJob: Job? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        wireApplicationGraph.inject(this)
        super.onCreate(savedInstanceState)
        setupOrientationForDevice()
        setUpCallingFlags()

        enableEdgeToEdge()

        val userId = intent.getStringExtra(EXTRA_USER_ID)
            ?.let(qualifiedIdMapper::fromStringToQualifiedID)
            ?: run {
            appLogger.e("$TAG missing call session user id, closing call activity")
            finish()
            return
        }

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
                is AppUserSessionPreparationResult.Ready -> showCall(userId, result.sessionScope)
                is AppUserSessionPreparationResult.Failed -> {
                    preparationState = UserSessionPreparationUiState.Failed(result.reason.toUiFailure())
                }
            }
        }
    }

    private fun showCall(userId: UserId, userSessionScope: UserSessionScope) {
        val sessionViewModelGraph = wireApplicationGraph.createSessionViewModelGraph(userId, userSessionScope)

        handleNewIntent(intent)
        onSessionPrepared()
        setUpScreenshotPreventionFlag()

        appLogger.i("$TAG Initializing proximity sensor..")
        proximitySensorManager.initialize()

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
                    Column(
                        modifier = Modifier.semantics { testTagsAsResourceId = true }
                    ) {
                        WireTopAppBar(
                            commonTopAppBarState = commonTopAppBarViewModel.state,
                            animateContentSize = false, // with animations, participant videos are blinking
                        )
                        Box(
                            modifier = Modifier.consumeWindowInsets(WindowInsets.statusBars)
                        ) {
                            Content()
                        }
                    }
                }
            }
        }
    }

    protected abstract fun handleNewIntent(intent: Intent)

    protected open fun onSessionPrepared() = Unit

    @Composable
    protected abstract fun Content()

    private fun updateTheApp() = launchUpdateTheApp()

    private fun openSupport() {
        val supportUrl = SupportUrlResolver.resolve(resources, SupportPage.SUPPORT)
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(supportUrl)))
    }

    fun switchAccountIfNeeded(userId: String?) {
        userId?.let {
            qualifiedIdMapper.fromStringToQualifiedID(it).run {
                callActivityViewModel.switchAccountIfNeeded(userId = this, actions = switchAccountObserver)
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

    fun setUpScreenshotPreventionFlag() {
        lifecycleScope.launch {
            if (callActivityViewModel.isScreenshotCensoringConfigEnabled().await()) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}
