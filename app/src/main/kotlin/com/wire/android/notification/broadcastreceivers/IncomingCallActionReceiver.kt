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
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.data.id.toQualifiedID
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IncomingCallActionReceiver : BroadcastReceiver() {

    @Suppress("ReturnCount")
    override fun onReceive(context: Context, intent: Intent) {
        val dependencies = context.broadcastReceiverDependencies
        val qualifiedIdMapper = dependencies.qualifiedIdMapper()
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

        dependencies.coroutineScope().launch(Dispatchers.Default) {
            with(dependencies.coreLogic().getSessionScope(userId)) {
                val conversationId = qualifiedIdMapper.fromStringToQualifiedID(conversationIdString)
                if (action == ACTION_DECLINE_CALL) {
                    calls.rejectCall(conversationId)
                }
            }
            dependencies.callNotificationManager().hideIncomingCallNotification(userId.toString(), conversationIdString)
        }
    }

    companion object {
        private const val EXTRA_CONVERSATION_ID = "conversation_id_extra"
        private const val EXTRA_RECEIVER_USER_ID = "user_id_extra"
        private const val EXTRA_ACTION = "action_extra"

        const val ACTION_DECLINE_CALL = "action_decline_call"

        fun newIntent(
            context: Context,
            conversationId: String,
            userId: String,
            action: String
        ): Intent = Intent(context, IncomingCallActionReceiver::class.java).apply {
            putExtra(EXTRA_CONVERSATION_ID, conversationId)
            putExtra(EXTRA_RECEIVER_USER_ID, userId)
            putExtra(EXTRA_ACTION, action)
        }
    }
}
