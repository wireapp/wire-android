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

import android.content.Context
import android.content.Intent
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.NoSession
import com.wire.android.di.metro.wireApplicationGraph
import com.wire.android.session.AppUserSessionPreparationResult
import com.wire.android.session.UserSessionPreparationGate
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.toQualifiedID
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dev.zacsweers.metro.Inject

class EndOngoingCallReceiver : CoroutineReceiver() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    @NoSession
    lateinit var qualifiedIdMapper: QualifiedIdMapper

    override fun onReceive(context: Context, intent: Intent?) {
        context.wireApplicationGraph.inject(this)
        super.onReceive(context, intent)
    }

    override suspend fun receive(context: Context, intent: Intent) {
        val conversationId: String = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: return
        appLogger.i("EndOngoingCallReceiver: onReceive, conversationId: $conversationId")

        val requestedUserId: QualifiedID? = intent.getStringExtra(EXTRA_RECEIVER_USER_ID)?.toQualifiedID(qualifiedIdMapper)
        val userId = requestedUserId ?: (coreLogic.globalScope { session.currentSession() } as? CurrentSessionResult.Success)
            ?.accountInfo
            ?.userId
        val sessionScope = userId?.let {
            when (val preparation = UserSessionPreparationGate(coreLogic).prepare(it)) {
                is AppUserSessionPreparationResult.Ready -> preparation.sessionScope
                is AppUserSessionPreparationResult.Failed -> {
                    appLogger.w("EndOngoingCallReceiver: session preparation failed: ${preparation.reason}")
                    null
                }
            }
        }

        sessionScope?.let {
            it.calls.endCall(qualifiedIdMapper.fromStringToQualifiedID(conversationId))
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
