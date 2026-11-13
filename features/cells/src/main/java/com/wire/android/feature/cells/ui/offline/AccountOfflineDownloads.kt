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
package com.wire.android.feature.cells.ui.offline

import com.wire.android.feature.cells.ui.BulkOfflineDownloadQueue
import com.wire.android.feature.cells.ui.BulkOfflineProgress
import com.wire.android.feature.cells.ui.CellError
import com.wire.android.feature.cells.ui.CellFileLocalPathCache
import com.wire.android.feature.cells.ui.OfflineDownloadStates
import com.wire.android.feature.cells.ui.OfflineFileDownloadController
import com.wire.android.feature.cells.ui.QueuedOfflineDownload
import com.wire.android.feature.cells.ui.downloadProgressOf
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.kalium.network.NetworkState
import com.wire.kalium.network.NetworkStateObserver
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.launch

/**
 * The offline downloads of one account. Lives in [OfflineDownloadManager], not in a screen, so downloads keep going
 * once the user leaves the screen or the app; [scope] is the manager's.
 *
 * Every request, a single file or a bulk selection, goes through the same [BulkOfflineDownloadQueue], so the
 * concurrency limits apply to all the downloads of the account at once.
 */
internal class AccountOfflineDownloads(
    private val scope: CoroutineScope,
    private val controller: OfflineFileDownloadController,
    private val pathCache: CellFileLocalPathCache,
    private val networkStateObserver: NetworkStateObserver,
) {
    private val nextRequestId = AtomicLong()

    private val queue: BulkOfflineDownloadQueue = BulkOfflineDownloadQueue(pathCache) { download, onSettled ->
        controller.start(
            scope = scope,
            cellNode = download.file,
            onSuccess = { localPath ->
                onSettled()
                download.onSaved(localPath)
            },
            onError = { error ->
                when (error) {
                    // Not settled for its request: it resumes once back online.
                    CellError.NO_NETWORK -> onConnectionLost(download)
                    CellError.NO_SPACE_LEFT -> onRanOutOfStorage()
                    else -> Unit
                }
                onSettled()
                if (error != CellError.NO_NETWORK) download.onFailed(error)
            },
            onCancel = {
                onSettled()
                download.onCancelled()
            },
            initialProgress = pathCache.offlineDownloadStates.value.paused[download.file.uuid],
        )
    }

    private var retryJob: Job? = null

    init {
        // Nothing starts while offline, and what paused starts again once back online.
        scope.launch {
            networkStateObserver.observeNetworkState().collect { state ->
                val isOnline = state is NetworkState.ConnectedWithInternet
                queue.setOnline(isOnline)
                // Their requests would otherwise only fail once they time out, holding their slots until then.
                if (!isOnline) controller.pauseAll()
            }
        }
    }

    /**
     * Parks [download] until the connection is back, which the network state observed in `init` resumes right away.
     *
     * The network state may report a connection though, leaving no change to wait for: the connection came back before
     * the request failed, or the state hasn't caught up with the loss yet. The paused downloads are then tried again
     * shortly, and pause again if still offline.
     */
    private fun onConnectionLost(download: QueuedOfflineDownload) {
        queue.setOnline(false)
        queue.requeuePaused(download)
        retryJob?.cancel()
        retryJob = scope.launch {
            if (isConnected()) {
                delay(RETRY_PAUSED_WHEN_CONNECTED_AFTER)
            } else {
                networkStateObserver.delayUntilConnectedWithInternetAgain(RETRY_PAUSED_AFTER)
            }
            queue.setOnline(isConnected())
        }
    }

    private fun isConnected(): Boolean =
        networkStateObserver.observeNetworkState().value is NetworkState.ConnectedWithInternet

    // The files still waiting would fail the same way. A function: the queue can't refer to itself while being created.
    private fun onRanOutOfStorage() {
        queue.cancelPending()
    }

    /** Download progress and queued files, what the file lists render. */
    val states: StateFlow<OfflineDownloadStates> = pathCache.offlineDownloadStates

    private val _events = MutableSharedFlow<OfflineDownloadEvent>(extraBufferCapacity = EVENTS_BUFFER)
    val events: SharedFlow<OfflineDownloadEvent> = _events.asSharedFlow()

    private val batch = MutableStateFlow(OfflineDownloadBatch())

    /** The downloads since this account last had none running, for the ongoing notification. */
    val summary: Flow<OfflineDownloadSummary> = combine(batch, states) { batch, states -> batch.toSummary(states) }

    fun availableStorageBytes(): Long = controller.availableStorageBytes()

    /**
     * Saves [files] for offline use. Files already downloading or queued are left out.
     * @return the id of the request, found again in [OfflineDownloadEvent.RequestFinished]; null when nothing was left to download.
     */
    fun download(files: List<CellNodeUi.File>): Long? {
        val newFiles = files
            .distinctBy { it.uuid }
            .filterNot { queue.contains(it.uuid) || controller.isDownloading(it.uuid) }
        if (newFiles.isEmpty()) return null

        val requestId = nextRequestId.incrementAndGet()
        val progress = BulkOfflineProgress(total = newFiles.size) { result ->
            _events.tryEmit(OfflineDownloadEvent.RequestFinished(requestId, result))
        }
        batch.update { it.plus(newFiles) }
        queue.enqueue(
            newFiles.map { file ->
                QueuedOfflineDownload(
                    file = file,
                    onSaved = { localPath ->
                        batch.update { it.settled(file.uuid, OfflineDownloadBatch.Outcome.SAVED) }
                        _events.tryEmit(OfflineDownloadEvent.FileSaved(file.uuid, localPath))
                        progress.onSaved()
                    },
                    onFailed = { error ->
                        batch.update {
                            it.settled(file.uuid, OfflineDownloadBatch.Outcome.FAILED)
                                .copy(ranOutOfStorage = it.ranOutOfStorage || error == CellError.NO_SPACE_LEFT)
                        }
                        progress.onFailed(error)
                    },
                    onCancelled = {
                        batch.update { it.settled(file.uuid, OfflineDownloadBatch.Outcome.CANCELLED) }
                        progress.onCancelled()
                    },
                )
            }
        )
        return requestId
    }

    /** Cancels the download of [uuid], whether it is running or still waiting for its turn. */
    fun cancel(uuid: String) {
        if (!queue.cancel(uuid)) controller.cancel(uuid, scope)
    }

    /** Cancels every download of the account, running or waiting. */
    fun cancelAll() {
        // Waiting ones first: cancelling a running download would let the next waiting one start.
        queue.cancelPending()
        controller.cancelAll(scope)
    }

    private companion object {
        const val EVENTS_BUFFER = 64
        val RETRY_PAUSED_AFTER = 30.seconds
        val RETRY_PAUSED_WHEN_CONNECTED_AFTER = 3.seconds
    }
}

/** What happened to the downloads of an [AccountOfflineDownloads], for the screen that asked for them. */
internal sealed interface OfflineDownloadEvent {
    /** A file was saved for offline use at [localPath]. */
    data class FileSaved(val uuid: String, val localPath: String) : OfflineDownloadEvent

    /** Every download of the request [requestId] settled, see [AccountOfflineDownloads.download]. */
    data class RequestFinished(val requestId: Long, val result: BulkOfflineProgress.Result) : OfflineDownloadEvent
}

/**
 * The downloads of an account since it last had none running. Starts over with the first download added once every
 * file of the previous batch settled.
 *
 * @param sizes the size of every file of the batch by uuid, null when unknown.
 */
internal data class OfflineDownloadBatch(
    val sizes: Map<String, Long?> = emptyMap(),
    val outcomes: Map<String, Outcome> = emptyMap(),
    val ranOutOfStorage: Boolean = false,
) {
    enum class Outcome { SAVED, FAILED, CANCELLED }

    private val isFinished: Boolean get() = outcomes.size >= sizes.size

    fun plus(files: List<CellNodeUi.File>): OfflineDownloadBatch {
        val base = if (isFinished) OfflineDownloadBatch() else this
        return base.copy(sizes = base.sizes + files.associate { it.uuid to it.size })
    }

    fun settled(uuid: String, outcome: Outcome): OfflineDownloadBatch =
        if (uuid in sizes) copy(outcomes = outcomes + (uuid to outcome)) else this

    fun toSummary(states: OfflineDownloadStates): OfflineDownloadSummary {
        val counted = sizes.filterKeys { outcomes[it] != Outcome.CANCELLED }
        val downloadedBytes = counted.entries.sumOf { (uuid, size) ->
            val knownSize = size?.coerceAtLeast(0L) ?: 0L
            when (outcomes[uuid]) {
                Outcome.SAVED -> knownSize
                null -> ((states.progresses.downloadProgressOf(uuid) ?: states.paused[uuid] ?: 0f) * knownSize).toLong()
                else -> 0L
            }
        }
        val pending = counted.keys.filter { it !in outcomes }
        return OfflineDownloadSummary(
            totalCount = counted.size,
            savedCount = outcomes.count { it.value == Outcome.SAVED },
            failedCount = outcomes.count { it.value == Outcome.FAILED },
            pendingCount = pending.size,
            downloadingCount = pending.count { it in states.progresses },
            pausedCount = pending.count { it in states.paused },
            totalBytes = counted.values.sumOf { it?.coerceAtLeast(0L) ?: 0L },
            downloadedBytes = downloadedBytes,
            ranOutOfStorage = ranOutOfStorage,
        )
    }
}
