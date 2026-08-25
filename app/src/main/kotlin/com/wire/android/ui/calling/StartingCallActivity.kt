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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.wire.android.navigation.style.TransitionAnimationType
import com.wire.android.ui.calling.CallActivity.Companion.EXTRA_CONVERSATION_ID
import com.wire.android.ui.calling.CallActivity.Companion.EXTRA_SCREEN_TYPE
import com.wire.android.ui.calling.CallActivity.Companion.EXTRA_SHOULD_ANSWER_CALL
import com.wire.android.ui.calling.CallActivity.Companion.EXTRA_USER_ID
import com.wire.android.ui.calling.incoming.IncomingCallScreen
import com.wire.android.ui.calling.ongoing.getOngoingCallIntent
import com.wire.android.ui.calling.outgoing.OutgoingCallScreen

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
class StartingCallActivity : CallActivity() {
    override val destination: CallActivityDestination = CallActivityDestination.STARTING

    @Composable
    override fun Content(request: CallActivityRequest) {
        val screen = (request.screen as? CallActivityScreen.Starting)?.type ?: return
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                TransitionAnimationType.POP_UP.enterTransition.togetherWith(
                    TransitionAnimationType.POP_UP.exitTransition
                )
            },
            modifier = Modifier.semantics { testTagsAsResourceId = true },
            label = screen.name
        ) { screenType ->
            when (screenType) {
                StartingCallScreenType.Outgoing -> {
                    OutgoingCallScreen(conversationId = request.conversationId) {
                        getOngoingCallIntent(
                            this@StartingCallActivity,
                            request.conversationId.toString(),
                            request.userId.toString(),
                        ).run {
                            this@StartingCallActivity.startActivity(this)
                        }
                        this@StartingCallActivity.finishAndRemoveTask()
                    }
                }

                StartingCallScreenType.Incoming -> {
                    IncomingCallScreen(
                        conversationId = request.conversationId,
                        shouldTryToAnswerCallAutomatically = request.shouldAnswerCall,
                    ) {
                        this@StartingCallActivity.startActivity(
                            getOngoingCallIntent(
                                this@StartingCallActivity,
                                request.conversationId.toString(),
                                request.userId.toString(),
                            )
                        )
                        this@StartingCallActivity.finishAndRemoveTask()
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

    override fun onDestroy() {
        cleanUpCallingFlags()
        super.onDestroy()
    }
}

fun getOutgoingCallIntent(
    context: Context,
    conversationId: String,
    userId: String,
) = Intent(context, StartingCallActivity::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    putExtra(EXTRA_USER_ID, userId)
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
