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
import android.widget.Toast
import androidx.core.app.RemoteInput
import com.wire.android.R
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.NoSession
import com.wire.android.notification.MessageNotificationManager
import com.wire.android.notification.NotificationConstants
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.common.functional.fold
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import dagger.hilt.android.AndroidEntryPoint
import kotlin.time.Clock
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReplyReceiver : CoroutineReceiver() { // requires zero argument constructor

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    @NoSession
    lateinit var qualifiedIdMapper: QualifiedIdMapper

    override suspend fun receive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val conversationId: String? = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        val userId: String? = intent.getStringExtra(EXTRA_USER_ID)

        if (remoteInput != null && conversationId != null && userId != null) {
            val replyText = remoteInput.getCharSequence(NotificationConstants.KEY_TEXT_REPLY).toString()
            val qualifiedUserId = qualifiedIdMapper.fromStringToQualifiedID(userId)
            val qualifiedConversationId = qualifiedIdMapper.fromStringToQualifiedID(conversationId)

            with(coreLogic.getSessionScope(qualifiedUserId)) {
                syncExecutor.request {
                    messages.sendTextMessage(qualifiedConversationId, replyText)
                        .fold(
                            { updateNotification(context, conversationId, qualifiedUserId, null) },
                            { updateNotification(context, conversationId, qualifiedUserId, replyText) }
                        )
                    conversations.updateConversationReadDateUseCase(
                        qualifiedConversationId,
                        Clock.System.now()
                    )
                }
            }
        }
    }

    override fun onTimeout(context: Context, intent: Intent, exception: Exception) {
        super.onTimeout(context, intent, exception)

        val conversationId: String? = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        val userId: String? = intent.getStringExtra(EXTRA_USER_ID)

        if (conversationId != null && userId != null) {
            val qualifiedUserId = qualifiedIdMapper.fromStringToQualifiedID(userId)
            updateNotification(context, conversationId, qualifiedUserId, null)
        }

        Toast.makeText(context, R.string.label_general_error, Toast.LENGTH_SHORT).show()
    }

    private fun updateNotification(context: Context, conversationId: String, userId: QualifiedID, replyText: String?) =
        MessageNotificationManager.updateNotificationAfterQuickReply(context, conversationId, userId, replyText)

    companion object {
        private const val EXTRA_CONVERSATION_ID = "conversation_id_extra"
        private const val EXTRA_USER_ID = "user_id_extra"

        fun newIntent(context: Context, conversationId: String, userId: String?): Intent =
            Intent(context, NotificationReplyReceiver::class.java).apply {
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(EXTRA_USER_ID, userId)
            }
    }
}
