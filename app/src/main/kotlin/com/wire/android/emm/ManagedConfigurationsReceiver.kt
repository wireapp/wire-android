/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.emm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wire.android.appLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagedConfigurationsReceiver @Inject constructor(
    private val managedConfigurationsRepository: ManagedConfigurationsRepository
) : BroadcastReceiver() {

    val logger = appLogger.withTextTag(TAG)

    override fun onReceive(context: Context, intent: Intent) {
        logger.i("onReceive called")
        managedConfigurationsRepository.getStringRestrictionByKey(ManagedConfigurationsKeys.TEST_KEY.asKey())?.let {
            logger.i("Received restriction test_key: $it")
        }
    }

    companion object {
        const val TAG = "ManagedConfigurationsReceiver"
    }
}
