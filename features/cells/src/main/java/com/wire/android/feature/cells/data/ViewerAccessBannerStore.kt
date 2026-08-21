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
package com.wire.android.feature.cells.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.wire.android.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Stores which conversations had their viewer access banner dismissed.
 * Once dismissed, the banner is not shown again for that conversation.
 */
@SingleIn(AppScope::class)
class ViewerAccessBannerStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)

    fun isDismissed(conversationId: String): Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[dismissedKey(conversationId)] ?: false }

    suspend fun setDismissed(conversationId: String) {
        context.dataStore.edit { preferences ->
            preferences[dismissedKey(conversationId)] = true
        }
    }

    private fun dismissedKey(conversationId: String) = booleanPreferencesKey("viewer_access_banner_dismissed_$conversationId")

    private companion object {
        const val PREFERENCES_NAME = "cells_viewer_access_banner"
    }
}
