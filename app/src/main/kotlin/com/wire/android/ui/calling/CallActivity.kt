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

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.wire.android.appLogger
import com.wire.android.navigation.style.TransitionAnimationType
import com.wire.android.notification.CallNotificationManager
import com.wire.android.ui.AppLockActivity
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.calling.incoming.IncomingCallScreen
import com.wire.android.ui.calling.initiating.InitiatingCallScreen
import com.wire.android.ui.calling.ongoing.OngoingCallScreen
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.theme.WireTheme
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallActivity : AppCompatActivity() {

    @Inject
    lateinit var callNotificationManager: CallNotificationManager

    @Inject
    lateinit var proximitySensorManager: ProximitySensorManager

    private val qualifiedIdMapper = QualifiedIdMapperImpl(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        callNotificationManager.hideAllNotifications()

        setUpCallingFlags()

        appLogger.i("$TAG Initializing proximity sensor..")
        proximitySensorManager.initialize()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val conversationId = intent.extras?.getString(EXTRA_CONVERSATION_ID)
        val screenType = intent.extras?.getString(EXTRA_SCREEN_TYPE)

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState,
                LocalActivity provides this
            ) {
                WireTheme {
                    var currentCallScreenType by remember { mutableStateOf(screenType) }
                    currentCallScreenType?.let { currentScreenType ->
                        AnimatedContent(
                            targetState = currentScreenType,
                            transitionSpec = {
                                TransitionAnimationType.POP_UP.enterTransition.togetherWith(
                                    TransitionAnimationType.POP_UP.exitTransition
                                )
                            },
                            label = currentScreenType
                        ) { screenType ->
                            conversationId?.let {
                                when (screenType) {
                                    CallScreenType.Initiating.name -> InitiatingCallScreen(
                                        qualifiedIdMapper.fromStringToQualifiedID(it)
                                    ) {
                                        currentCallScreenType = CallScreenType.Ongoing.name
                                    }

                                    CallScreenType.Ongoing.name -> OngoingCallScreen(
                                        qualifiedIdMapper.fromStringToQualifiedID(it)
                                    )

                                    CallScreenType.Incoming.name -> IncomingCallScreen(
                                        qualifiedIdMapper.fromStringToQualifiedID(it)
                                    ) {
                                        currentCallScreenType = CallScreenType.Ongoing.name
                                    }
                                }
                            }
                        }
                    } ?: run {
                        finish()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        proximitySensorManager.registerListener()
    }

    override fun onPause() {
        super.onPause()
        proximitySensorManager.unRegisterListener()
    }

    companion object {
        private const val TAG = "CallActivity"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_SCREEN_TYPE = "screen_type"
    }
}

fun CallActivity.setUpCallingFlags() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    } else {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON,
        )
    }
}

fun getOngoingCallIntent(
    activity: Activity,
    conversationId: String
) = Intent(activity, CallActivity::class.java).apply {
    putExtra(CallActivity.EXTRA_CONVERSATION_ID, conversationId)
    putExtra(CallActivity.EXTRA_SCREEN_TYPE, CallScreenType.Ongoing.name)
}

fun getInitiatingCallIntent(
    activity: Activity,
    conversationId: String
) = Intent(activity, CallActivity::class.java).apply {
    putExtra(CallActivity.EXTRA_CONVERSATION_ID, conversationId)
    putExtra(CallActivity.EXTRA_SCREEN_TYPE, CallScreenType.Initiating.name)
}

fun CallActivity.openAppLockActivity() {
    Intent(this, AppLockActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
    }.run {
        startActivity(this)
    }
}
