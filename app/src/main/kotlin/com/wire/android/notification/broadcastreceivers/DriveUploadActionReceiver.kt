/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.NoSession
import com.wire.android.di.metro.wireApplicationGraph
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.toQualifiedID
import dev.zacsweers.metro.Inject

/**
 * Handles the "Cancel all" / "Retry" actions on the Shared Drive upload notification.
 *
 * [CellUploadCoordinator][com.wire.kalium.cells.domain.CellUploadCoordinator] methods are plain
 * (non-suspend) function calls, so this runs synchronously in [onReceive] with no need for `goAsync()`
 * or a background coroutine.
 */
class DriveUploadActionReceiver : BroadcastReceiver() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: CoreLogic

    @Inject
    @NoSession
    lateinit var qualifiedIdMapper: QualifiedIdMapper

    override fun onReceive(context: Context, intent: Intent) {
        context.wireApplicationGraph.inject(this)
        val userId = intent.getStringExtra(EXTRA_USER_ID)?.toQualifiedID(qualifiedIdMapper) ?: return
        val uploadCoordinator = coreLogic.getSessionScope(userId).cells.uploadCoordinator

        when (intent.getStringExtra(EXTRA_ACTION)) {
            ACTION_CANCEL_ALL -> uploadCoordinator.cancelAll()
            ACTION_RETRY_FAILED -> uploadCoordinator.retryAllFailed()
        }
    }

    companion object {
        private const val EXTRA_USER_ID = "user_id_extra"
        private const val EXTRA_ACTION = "action_extra"
        const val ACTION_CANCEL_ALL = "cancel_all"
        const val ACTION_RETRY_FAILED = "retry_failed"

        fun newIntent(context: Context, userId: String, action: String): Intent =
            Intent(context, DriveUploadActionReceiver::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_ACTION, action)
            }
    }
}