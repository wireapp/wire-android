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
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.toQualifiedID
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EndOngoingCallReceiver : BroadcastReceiver() {

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

    override fun onReceive(context: Context, intent: Intent) {
        val conversationId: String = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: return
        appLogger.i("EndOngoingCallReceiver: onReceive, conversationId: $conversationId")

        coroutineScope.launch {
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
