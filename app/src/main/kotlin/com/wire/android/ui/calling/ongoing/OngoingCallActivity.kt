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
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Rational
import androidx.activity.compose.setContent
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
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.navigation.style.TransitionAnimationType
import com.wire.android.notification.CallNotificationManager
import com.wire.android.notification.endOngoingCallPendingIntent
import com.wire.android.services.ServicesManager
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.calling.CallActivity
import com.wire.android.ui.calling.CallActivity.Companion.EXTRA_CONVERSATION_ID
import com.wire.android.ui.calling.CallActivity.Companion.EXTRA_SHOULD_ANSWER_CALL
import com.wire.android.ui.calling.CallActivity.Companion.EXTRA_USER_ID
import com.wire.android.ui.calling.common.ProximitySensorManager
import com.wire.android.ui.calling.ongoing.OngoingCallActivity.Companion.TAG
import com.wire.android.ui.common.setupOrientationForDevice
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.theme.WireTheme
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
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
@OptIn(ExperimentalComposeUiApi::class)
@AndroidEntryPoint
class OngoingCallActivity : CallActivity() {
    @Inject
    lateinit var proximitySensorManager: ProximitySensorManager

    @Inject
    lateinit var servicesManager: ServicesManager

    @Inject
    lateinit var callNotificationManager: CallNotificationManager

    var conversationId: String? by mutableStateOf(null)
    var userId: String? by mutableStateOf(null)
    private var shouldAnswerCall: Boolean by mutableStateOf(false)

    private fun handleNewIntent(intent: Intent) {
        conversationId = intent.extras?.getString(EXTRA_CONVERSATION_ID)
        userId = intent.extras?.getString(EXTRA_USER_ID)
        shouldAnswerCall = intent.extras?.getBoolean(EXTRA_SHOULD_ANSWER_CALL) ?: false
        require(conversationId != null) { "$TAG No conversation ID provided in intent extras" }
        require(userId != null) { "$TAG No user ID provided in intent extras" }
        switchAccountIfNeeded(userId)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNewIntent(intent)
        setIntent(intent)
    }

    @SuppressLint("UnusedContentLambdaTargetStateParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupOrientationForDevice()
        setUpScreenshotPreventionFlag()
        setUpCallingFlags()

        handleNewIntent(intent)

        if (shouldAnswerCall && userId != null && conversationId != null) {
            callNotificationManager.hideIncomingCallNotification(userId!!, conversationId!!)
            servicesManager.startCallServiceToAnswer(
                qualifiedIdMapper.fromStringToQualifiedID(userId!!),
                qualifiedIdMapper.fromStringToQualifiedID(conversationId!!),
            )
        }

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
                            modifier = Modifier.semantics { testTagsAsResourceId = true },
                            label = TAG
                        ) { _ ->
                            OngoingCallScreen(
                                qualifiedIdMapper.fromStringToQualifiedID(
                                    conversationId
                                )
                            )
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
        const val TAG = "OngoingCallActivity"
    }
}

fun getOngoingCallIntent(
    context: Context,
    conversationId: String,
    userId: String,
    shouldAnswerCall: Boolean = false,
) = Intent(context, OngoingCallActivity::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    putExtra(EXTRA_CONVERSATION_ID, conversationId)
    putExtra(EXTRA_USER_ID, userId)
    putExtra(EXTRA_SHOULD_ANSWER_CALL, shouldAnswerCall)
}

private const val ASPECT_RATIO_NUMERATOR = 2
private const val ASPECT_RATIO_DENOMINATOR = 3

fun OngoingCallActivity.enterPiPMode(conversationId: ConversationId, userId: UserId) {
    appLogger.i("$TAG: Entering Picture-in-Picture mode..")
    val hangupAction = RemoteAction(
        Icon.createWithResource(this, R.drawable.ic_call_end),
        getString(R.string.calling_hang_up_call),
        getString(R.string.content_description_calling_hang_up_call),
        endOngoingCallPendingIntent(this, conversationId.toString(), userId.toString())
    )

    val pictureInPictureParams = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(ASPECT_RATIO_NUMERATOR, ASPECT_RATIO_DENOMINATOR))
        .setActions(listOf(hangupAction))
        .build()
    this.enterPictureInPictureMode(pictureInPictureParams)
}
