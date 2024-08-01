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
package com.wire.android.ui.calling.ongoing

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import com.wire.android.appLogger
import com.wire.android.navigation.style.TransitionAnimationType
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.calling.CallActivity
import com.wire.android.ui.calling.CallActivity.Companion.EXTRA_CONVERSATION_ID
import com.wire.android.ui.calling.ProximitySensorManager
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.theme.WireTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity that handles ongoing call screen, Ongoing.
 * These type of call is not disposable and we need to maintain its state.
 *
 * This screen is used when the self user is in a call.
 *
 * @see OngoingCallScreen
 */
@AndroidEntryPoint
class OngoingCallActivity : CallActivity() {
    @Inject
    lateinit var proximitySensorManager: ProximitySensorManager

    @SuppressLint("UnusedContentLambdaTargetStateParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpScreenshotPreventionFlag()
        setUpCallingFlags()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val conversationId = intent.extras?.getString(EXTRA_CONVERSATION_ID)
        val userId = intent.extras?.getString(EXTRA_USER_ID)
        switchAccountIfNeeded(userId)

        appLogger.i("$TAG Initializing proximity sensor..")
        proximitySensorManager.initialize()

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState,
                LocalActivity provides this
            ) {
                WireTheme {
                    conversationId?.let { conversationId ->
                        AnimatedContent(
                            targetState = TAG,
                            transitionSpec = {
                                TransitionAnimationType.POP_UP.enterTransition.togetherWith(
                                    TransitionAnimationType.POP_UP.exitTransition
                                )
                            },
                            label = TAG
                        ) { _ ->
                            OngoingCallScreen(qualifiedIdMapper.fromStringToQualifiedID(conversationId))
                        }
                    } ?: run { finish() }
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

    override fun onDestroy() {
        cleanUpCallingFlags()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "OngoingCallActivity"
    }
}

fun getOngoingCallIntent(
    activity: Activity,
    conversationId: String
) = Intent(activity, OngoingCallActivity::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    putExtra(EXTRA_CONVERSATION_ID, conversationId)
}
