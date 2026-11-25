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

import com.wire.android.feature.cells.ui.dialog.EmptyRecycleBinDialog
import com.wire.kalium.cells.domain.usecase.EmptyRecycleBinUseCase
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Permanently deletes everything in the recycle bin of a conversation, once confirmed. One instance per screen. */
class EmptyRecycleBinController @Inject constructor(
    private val emptyRecycleBinUseCase: EmptyRecycleBinUseCase,
) {

    // Kept in the state, not sent as actions, so the dialog survives configuration changes.
    private val _dialog = MutableStateFlow<EmptyRecycleBinDialog?>(null)
    internal val dialog: StateFlow<EmptyRecycleBinDialog?> = _dialog.asStateFlow()

    internal fun showConfirmation() {
        _dialog.value = EmptyRecycleBinDialog.Confirmation()
    }

    /**
     * Empties the recycle bin in a single request. There is no rollback: if it fails midway, some items may still have
     * been deleted, so the list is refreshed either way.
     */
    @Suppress("ReturnCount")
    internal fun emptyConfirmed(host: CellScreenHost) {
        val confirmation = _dialog.value as? EmptyRecycleBinDialog.Confirmation ?: return
        if (confirmation.isInProgress) return
        val conversationId = host.context.rootConversationId ?: return
        _dialog.value = confirmation.copy(isInProgress = true)
        host.scope.launch {
            emptyRecycleBinUseCase(conversationId)
                .onSuccess {
                    _dialog.value = null
                    host.sendAction(ShowRecycleBinEmptiedMessage)
                }
                .onFailure { failure ->
                    _dialog.value = EmptyRecycleBinDialog.Failure(
                        isNoNetworkConnection = failure is NetworkFailure.NoNetworkConnection,
                    )
                }
            host.nodeList.refresh()
        }
    }

    internal fun dismissDialog() {
        // The confirmation stays up until the request completes.
        if ((_dialog.value as? EmptyRecycleBinDialog.Confirmation)?.isInProgress == true) return
        _dialog.value = null
    }
}
