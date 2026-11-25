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

import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.OpenLoadState
import com.wire.kalium.cells.domain.model.Node
import com.wire.kalium.cells.domain.usecase.CellNodeAvailability
import com.wire.kalium.cells.domain.usecase.CheckCellNodeAvailabilityUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The lists aren't updated when nodes change elsewhere: before opening a node from the server or acting on it, check
 * it's still there, so a deleted one isn't opened into a viewer that can only fail, or acted on for nothing.
 * One instance per screen: it holds the checks running.
 */
class CellNodeAvailabilityChecker @Inject constructor(
    private val checkCellNodeAvailability: CheckCellNodeAvailabilityUseCase,
    private val sharedPathCache: CellFileLocalPathCache,
) {

    // Availability checks running, by uuid.
    private val checks = mutableMapOf<String, Job>()

    /**
     * Runs [action] on [node] if it's still there. A file then goes on with fresh URLs, as the listed ones may have
     * expired. If it can't be checked, e.g. offline, it goes on as before. If it's gone, it is taken off the list.
     */
    internal fun <T : CellNodeUi> ifAvailable(node: T, isOnline: Boolean, host: CellScreenHost, action: (T) -> Unit) {
        if (!isOnline) {
            action(node)
            return
        }
        if (node.uuid in checks) return
        val check = host.scope.launch {
            // Like a download, only show the spinner if the check is slow.
            val showSpinnerJob = launch {
                delay(OpenFileDownloadController.SPINNER_THRESHOLD_MS)
                sharedPathCache.setOpenLoadState(node.uuid, OpenLoadState.Loading())
            }
            val availability = checkCellNodeAvailability(node.uuid, inRecycleBin = host.context.isRecycleBin)
            showSpinnerJob.cancel()
            sharedPathCache.clearOpenLoadState(node.uuid)
            when (availability) {
                is CellNodeAvailability.Available -> action(node.updatedWith(availability.node))
                is CellNodeAvailability.Gone -> onNodeNoLongerAvailable(node, host)
                is CellNodeAvailability.Unknown -> action(node)
            }
        }
        checks[node.uuid] = check
        // Only if not replaced meanwhile, by a new tap after this check was cancelled.
        check.invokeOnCompletion { if (checks[node.uuid] === check) checks.remove(node.uuid) }
    }

    /** Cancels the check running for [uuid], if any. */
    fun cancel(uuid: String) {
        checks.remove(uuid)?.let { check ->
            check.cancel()
            sharedPathCache.clearOpenLoadState(uuid)
        }
    }

    /** Its local data was already cleaned up: take it off the list, which is likely outdated in other ways too. */
    private fun onNodeNoLongerAvailable(node: CellNodeUi, host: CellScreenHost) {
        sharedPathCache.clearCompletedPath(node.uuid)
        host.nodeList.hide(node)
        host.sendAction(ShowError(CellError.ITEM_NO_LONGER_AVAILABLE))
        host.nodeList.refresh()
    }

    // A file copy is still a T: T is either CellNodeUi.File or CellNodeUi itself.
    @Suppress("UNCHECKED_CAST")
    private fun <T : CellNodeUi> T.updatedWith(current: Node): T = when {
        this is CellNodeUi.File && current is Node.File -> copy(
            contentUrl = current.contentUrl ?: contentUrl,
            previewUrl = current.previewUrl ?: previewUrl,
            pdfPreviewUrl = current.pdfPreviewUrl ?: pdfPreviewUrl,
        ) as T

        else -> this
    }
}
