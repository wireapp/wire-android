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
package com.wire.android.feature.privacy.data

import android.content.Context
import com.wire.android.di.ApplicationContext
import com.wire.kalium.logic.data.user.UserId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Caches one [ConversationPrivacyStore] per user, mirroring
 * [com.wire.android.datastore.UserDataStoreProvider]. App-scoped so the DataStore instances are shared.
 */
@SingleIn(AppScope::class)
class ConversationPrivacyStoreProvider @Inject constructor(@ApplicationContext private val context: Context) {

    private val stores: ConcurrentMap<UserId, ConversationPrivacyStore> by lazy { ConcurrentHashMap() }

    fun getOrCreate(userId: UserId): ConversationPrivacyStore =
        stores.computeIfAbsent(userId) { ConversationPrivacyStore(context, userId) }
}
