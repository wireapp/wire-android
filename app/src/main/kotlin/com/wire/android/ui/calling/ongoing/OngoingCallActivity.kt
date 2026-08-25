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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.wire.android.di.metro.wireApplicationGraph
import com.wire.android.navigation.style.TransitionAnimationType
import com.wire.android.notification.CallNotificationManager
import com.wire.android.services.ServicesManager
import com.wire.android.ui.calling.CallActivity
import com.wire.android.ui.calling.CallActivityDestination
import com.wire.android.ui.calling.CallActivityRequest
import com.wire.android.ui.calling.CallActivity.Companion.EXTRA_CONVERSATION_ID
import com.wire.android.ui.calling.CallActivity.Companion.EXTRA_SHOULD_ANSWER_CALL
import com.wire.android.ui.calling.CallActivity.Companion.EXTRA_USER_ID
import com.wire.android.ui.calling.ongoing.OngoingCallActivity.Companion.TAG
import dev.zacsweers.metro.Inject

/**
 * Activity that handles ongoing call screen, Ongoing.
 * These type of call is not disposable and we need to maintain its state.
 *
 * This screen is used when the self user is in a call.
 *
 * @see OngoingCallScreen
 */
@OptIn(ExperimentalComposeUiApi::class)
class OngoingCallActivity : CallActivity() {
    @Inject
    lateinit var servicesManager: ServicesManager

    @Inject
    lateinit var callNotificationManager: CallNotificationManager

    override val destination: CallActivityDestination = CallActivityDestination.ONGOING

    override fun onCreate(savedInstanceState: Bundle?) {
        wireApplicationGraph.inject(this)
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun Content(request: CallActivityRequest) {
        LaunchedEffect(request) {
            if (request.shouldAnswerCall) {
                callNotificationManager.hideIncomingCallNotification(
                    request.userId.toString(),
                    request.conversationId.toString(),
                )
                servicesManager.startCallServiceToAnswer(request.userId, request.conversationId)
            }
        }
        AnimatedContent(
            targetState = request.conversationId,
            transitionSpec = {
                TransitionAnimationType.POP_UP.enterTransition.togetherWith(
                    TransitionAnimationType.POP_UP.exitTransition
                )
            },
            modifier = Modifier.semantics { testTagsAsResourceId = true },
            label = TAG
        ) { conversationId ->
            OngoingCallScreen(conversationId)
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
