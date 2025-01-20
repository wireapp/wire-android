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

package com.wire.android.notification.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wire.android.appLogger
import com.wire.android.di.ApplicationScope
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.NoSession
import com.wire.android.notification.CallNotificationManager
import com.wire.android.services.ServicesManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.toQualifiedID
import com.wire.kalium.logic.data.user.UserId
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class IncomingCallActionReceiver : BroadcastReceiver() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    @NoSession
    lateinit var qualifiedIdMapper: QualifiedIdMapper

    @Inject
    @ApplicationScope
    lateinit var coroutineScope: CoroutineScope

    @Inject
    lateinit var callNotificationManager: CallNotificationManager

    @Inject
    lateinit var servicesManager: ServicesManager

    @Suppress("ReturnCount")
    override fun onReceive(context: Context, intent: Intent) {
        val conversationIdString: String = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: run {
            appLogger.e("CallNotificationDismissReceiver: onReceive, conversation ID is missing")
            return
        }
        appLogger.i("CallNotificationDismissReceiver: onReceive, conversationId: ${conversationIdString.obfuscateId()}")
        val userId: UserId = intent.getStringExtra(EXTRA_RECEIVER_USER_ID)?.toQualifiedID(qualifiedIdMapper) ?: run {
            appLogger.e("CallNotificationDismissReceiver: onReceive, user ID is missing")
            return
        }
        val action: String = intent.getStringExtra(EXTRA_ACTION) ?: run {
            appLogger.e("CallNotificationDismissReceiver: onReceive, action is missing")
            return
        }

        coroutineScope.launch(Dispatchers.Default) {
            with(coreLogic.getSessionScope(userId)) {
                val conversationId = qualifiedIdMapper.fromStringToQualifiedID(conversationIdString)
                when (action) {
                    ACTION_DECLINE_CALL -> calls.rejectCall(conversationId)
                    ACTION_ANSWER_CALL -> servicesManager.startCallServiceToAnswer(userId, conversationId)
                }
            }
            callNotificationManager.hideIncomingCallNotification(userId.toString(), conversationIdString)
        }
    }

    companion object {
        private const val EXTRA_CONVERSATION_ID = "conversation_id_extra"
        private const val EXTRA_RECEIVER_USER_ID = "user_id_extra"
        private const val EXTRA_ACTION = "action_extra"

        const val ACTION_DECLINE_CALL = "action_decline_call"
        const val ACTION_ANSWER_CALL = "action_answer_call"

        fun newIntent(
            context: Context,
            conversationId: String,
            userId: String,
            action: String
        ): Intent =
            Intent(context, IncomingCallActionReceiver::class.java).apply {
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(EXTRA_RECEIVER_USER_ID, userId)
                putExtra(EXTRA_ACTION, action)
            }
    }
}
