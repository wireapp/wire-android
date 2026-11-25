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
package com.wire.android.feature.cells.ui

import com.wire.android.datastore.UserDataStore
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

/** The banners and tips of the files screens, each shown until dismissed. Dismissals are remembered. */
class CellScreenHints @Inject constructor(
    private val userDataStore: UserDataStore,
) {

    /**
     * Viewer access banner, shown while browsing files of a conversation the self user only has viewer access to.
     * Dismissal is remembered for that conversation.
     */
    fun viewerAccessBanner(conversationId: String?, isViewerOnly: Flow<Boolean>): Flow<Boolean> = combine(
        isViewerOnly,
        conversationId?.let { userDataStore.isViewerAccessBannerDismissed(it) } ?: flowOf(true),
    ) { viewerOnly, dismissed -> viewerOnly && !dismissed }

    suspend fun dismissViewerAccessBanner(conversationId: String) = userDataStore.setViewerAccessBannerDismissed(conversationId)

    /** The bulk selection tip, shown wherever selection is available. */
    fun bulkSelectionHint(isSelectionEnabled: Flow<Boolean>): Flow<Boolean> = combine(
        isSelectionEnabled,
        userDataStore.isBulkSelectionHintDismissed(),
    ) { enabled, dismissed -> enabled && !dismissed }

    suspend fun dismissBulkSelectionHint() = userDataStore.setBulkSelectionHintDismissed()

    /** The tip pointing at the offline toggle, waiting for the bulk selection tip to be gone: one tip at a time. */
    fun offlineToggleHint(isToggleShown: Flow<Boolean>, isBulkSelectionHintShown: Flow<Boolean>): Flow<Boolean> = combine(
        isToggleShown,
        isBulkSelectionHintShown,
        userDataStore.isOfflineToggleHintDismissed(),
    ) { toggleShown, bulkSelectionHintShown, dismissed -> toggleShown && !bulkSelectionHintShown && !dismissed }

    suspend fun dismissOfflineToggleHint() = userDataStore.setOfflineToggleHintDismissed()
}
