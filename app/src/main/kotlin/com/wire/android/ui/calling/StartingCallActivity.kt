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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.wire.android.appLogger
import com.wire.android.navigation.style.TransitionAnimationType
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.calling.CallActivity.Companion.EXTRA_CONVERSATION_ID
import com.wire.android.ui.calling.CallActivity.Companion.EXTRA_SCREEN_TYPE
import com.wire.android.ui.calling.CallActivity.Companion.EXTRA_SHOULD_ANSWER_CALL
import com.wire.android.ui.calling.CallActivity.Companion.EXTRA_USER_ID
import com.wire.android.ui.calling.incoming.IncomingCallScreen
import com.wire.android.ui.calling.ongoing.getOngoingCallIntent
import com.wire.android.ui.calling.outgoing.OutgoingCallScreen
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.theme.WireTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity that handles starting call screens, Incoming and Outgoing
 * These type of call steps are one shot disposable screens.
 *
 * This screen is used when the self user starts a call or when the self user receives a call.
 *
 * @see IncomingCallScreen
 * @see OutgoingCallScreen
 */
@OptIn(ExperimentalComposeUiApi::class)
@AndroidEntryPoint
class StartingCallActivity : CallActivity() {
    @Inject
    lateinit var proximitySensorManager: ProximitySensorManager

    private var conversationId: String? by mutableStateOf(null)
    private var userId: String? by mutableStateOf(null)
    private var screenType: StartingCallScreenType? by mutableStateOf(null)
    private var shouldAnswerCall: Boolean by mutableStateOf(false)

    private fun handleNewIntent(intent: Intent) {
        conversationId = intent.extras?.getString(EXTRA_CONVERSATION_ID)
        userId = intent.extras?.getString(EXTRA_USER_ID)
        screenType = intent.extras?.getString(EXTRA_SCREEN_TYPE)?.let { StartingCallScreenType.byName(it) }
        shouldAnswerCall = intent.extras?.getBoolean(EXTRA_SHOULD_ANSWER_CALL, false) ?: false
        require(conversationId != null) { "$TAG No conversation ID provided in intent extras" }
        require(userId != null) { "$TAG No user ID provided in intent extras" }
        require(screenType != null) { "$TAG No screen type provided in intent extras" }
        switchAccountIfNeeded(userId)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNewIntent(intent)
        setIntent(intent)
    }

    @Suppress("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setUpScreenshotPreventionFlag()
        setUpCallingFlags()

        enableEdgeToEdge()

        handleNewIntent(intent)

        appLogger.i("$TAG Initializing proximity sensor..")
        proximitySensorManager.initialize()

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState,
                LocalActivity provides this
            ) {
                WireTheme {
                    screenType?.let { currentScreenType ->
                        AnimatedContent(
                            targetState = currentScreenType,
                            transitionSpec = {
                                TransitionAnimationType.POP_UP.enterTransition.togetherWith(
                                    TransitionAnimationType.POP_UP.exitTransition
                                )
                            },
                            modifier = Modifier.semantics { testTagsAsResourceId = true },
                            label = currentScreenType.name
                        ) { screenType ->
                            conversationId?.let {
                                when (screenType) {
                                    StartingCallScreenType.Outgoing -> {
                                        OutgoingCallScreen(
                                            conversationId = qualifiedIdMapper.fromStringToQualifiedID(it)
                                        ) {
                                            getOngoingCallIntent(this@StartingCallActivity, it).run {
                                                this@StartingCallActivity.startActivity(this)
                                            }
                                            this@StartingCallActivity.finishAndRemoveTask()
                                        }
                                    }

                                    StartingCallScreenType.Incoming -> {
                                        IncomingCallScreen(
                                            conversationId = qualifiedIdMapper.fromStringToQualifiedID(it),
                                            shouldTryToAnswerCallAutomatically = shouldAnswerCall,
                                        ) {
                                            this@StartingCallActivity.startActivity(
                                                getOngoingCallIntent(this@StartingCallActivity, it)
                                            )
                                            this@StartingCallActivity.finishAndRemoveTask()
                                        }
                                    }
                                }
                            }
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
        private const val TAG = "StartingCallActivity"
    }
}

fun getOutgoingCallIntent(
    context: Context,
    conversationId: String
) = Intent(context, StartingCallActivity::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    putExtra(EXTRA_CONVERSATION_ID, conversationId)
    putExtra(EXTRA_SCREEN_TYPE, StartingCallScreenType.Outgoing.name)
}

fun getIncomingCallIntent(
    context: Context,
    conversationId: String,
    userId: String,
    shouldAnswerCall: Boolean = false
) = Intent(context.applicationContext, StartingCallActivity::class.java).apply {
    putExtra(EXTRA_USER_ID, userId)
    putExtra(EXTRA_CONVERSATION_ID, conversationId)
    putExtra(EXTRA_SCREEN_TYPE, StartingCallScreenType.Incoming.name)
    putExtra(EXTRA_SHOULD_ANSWER_CALL, shouldAnswerCall)
}
