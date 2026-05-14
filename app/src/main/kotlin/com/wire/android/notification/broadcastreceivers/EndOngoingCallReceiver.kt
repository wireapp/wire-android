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
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.toQualifiedID
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import kotlinx.coroutines.launch

class EndOngoingCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val dependencies = context.broadcastReceiverDependencies
        val coreLogic = dependencies.coreLogic()
        val qualifiedIdMapper = dependencies.qualifiedIdMapper()
        val conversationId: String = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: return
        appLogger.i("EndOngoingCallReceiver: onReceive, conversationId: $conversationId")

        dependencies.coroutineScope().launch {
            val userId: QualifiedID? = intent.getStringExtra(EXTRA_RECEIVER_USER_ID)?.toQualifiedID(qualifiedIdMapper)
            val sessionScope =
                if (userId != null) {
                    coreLogic.getSessionScope(userId)
                } else {
                    val currentSession = coreLogic.globalScope { session.currentSession() }
                    if (currentSession is CurrentSessionResult.Success) {
                        coreLogic.getSessionScope(currentSession.accountInfo.userId)
                    } else {
                        null
                    }
                }

            sessionScope?.let {
                it.calls.endCall(qualifiedIdMapper.fromStringToQualifiedID(conversationId))
            }
        }
    }

    companion object {
        private const val EXTRA_CONVERSATION_ID = "conversation_id_extra"
        private const val EXTRA_RECEIVER_USER_ID = "user_id_extra"

        fun newIntent(context: Context, conversationId: String?, userId: String?): Intent =
            Intent(context, EndOngoingCallReceiver::class.java).apply {
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(EXTRA_RECEIVER_USER_ID, userId)
            }
    }
}
