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
import com.wire.android.notification.CallNotificationIds
import com.wire.android.notification.CallNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CallNotificationDismissedReceiver : BroadcastReceiver() { // requires zero argument constructor

    @Inject
    lateinit var callNotificationManager: CallNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        val conversationIdString: String = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: return
        val userIdString: String = intent.getStringExtra(EXTRA_USER_ID) ?: return
        appLogger.i("CallNotificationDismissedReceiver: onReceive")
        callNotificationManager.reloadCallNotifications(CallNotificationIds(userIdString, conversationIdString))
    }

    companion object {
        private const val EXTRA_CONVERSATION_ID = "conversation_id_extra"
        private const val EXTRA_USER_ID = "user_id_extra"

        fun newIntent(context: Context, conversationIdString: String?, userIdString: String?): Intent =
            Intent(context, CallNotificationDismissedReceiver::class.java).apply {
                putExtra(EXTRA_CONVERSATION_ID, conversationIdString)
                putExtra(EXTRA_USER_ID, userIdString)
            }
    }
}
