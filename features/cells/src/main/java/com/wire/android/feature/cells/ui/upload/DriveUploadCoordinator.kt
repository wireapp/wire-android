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
package com.wire.android.feature.cells.ui.upload

import com.wire.android.di.metro.MetroSessionScope
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.cells.domain.CellUploadEvent
import com.wire.kalium.cells.domain.CellUploadManager
import com.wire.kalium.common.functional.nullableFold
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.UUID

// TODO: to cover it with unit test in a different PR
/**
 * Schedules direct uploads of user-picked files into Shared Drive folders.
 *
 * The actual transfer is delegated to `CellUploadManager`, which owns pre-check, draft node creation
 * and the (multipart) S3 upload. This class only adds what that manager does not provide: an ordered
 * queue, admission control, aggregate per-file state, cancellation and retry.
 *
 * Scoped to [MetroSessionScope] so a batch keeps running while the user browses other folders and is
 * discarded on logout, mirroring [com.wire.android.feature.cells.ui.CellFileLocalPathCache].
 *
 * All state lives in [uploads] and is mutated exclusively by the single scheduler coroutine started in
 * `init`, which drains [commands]. Public methods only post commands, so queue and slot accounting are
 * free of locks and process in a deterministic order.
 */
@Suppress("TooManyFunctions")
@SingleIn(MetroSessionScope::class)
class DriveUploadCoordinator @Inject constructor(
    private val uploadManager: CellUploadManager,
    dispatcherProvider: DispatcherProvider,
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io())

    private val _uploads = MutableStateFlow<List<DriveUploadItem>>(emptyList())
    val uploads: StateFlow<List<DriveUploadItem>> = _uploads.asStateFlow()

    // UNLIMITED so posting a command never suspends: upload workers post from inside event collection,
    // and a worker blocked on send while the scheduler waits on that same worker would deadlock.
    private val commands = Channel<Command>(Channel.UNLIMITED)

    private val workers = mutableMapOf<String, Job>()

    init {
        scope.launch {
            for (command in commands) {
                handle(command)
                startEligibleUploads()
            }
        }
    }

    /** Adds [requests] to the end of the queue. They start as soon as the concurrency rules allow. */
    fun enqueue(requests: List<DriveUploadRequest>) {
        commands.trySend(Command.Enqueue(requests))
    }

    /** Cancels a queued or running upload, aborting the underlying transfer. */
    fun cancel(id: String) {
        commands.trySend(Command.Cancel(id))
    }

    /** Cancels every queued and running upload. */
    fun cancelAll() {
        commands.trySend(Command.CancelAll)
    }

    /** Re-queues a failed upload. Completed and running uploads are untouched. */
    fun retry(id: String) {
        commands.trySend(Command.Retry(id))
    }

    /** Re-queues every failed upload, in their original order. */
    fun retryAllFailed() {
        commands.trySend(Command.RetryAllFailed)
    }

    private suspend fun handle(command: Command) {
        when (command) {
            is Command.Enqueue -> addItems(command.requests)
            is Command.Cancel -> cancelItem(command.id)
            Command.CancelAll -> cancelAllItems()
            is Command.Retry -> requeueFailed(command.id)
            Command.RetryAllFailed -> requeueAllFailed()
            is Command.NodeCreated -> updateItem(command.id) { copy(nodeUuid = command.nodeUuid) }
            is Command.Progress -> updateProgress(command.id, command.progress)
            is Command.Finished -> finishItem(command.id, command.state)
        }
    }

    private fun addItems(requests: List<DriveUploadRequest>) {
        val items = requests.map { DriveUploadItem(id = UUID.randomUUID().toString(), request = it) }
        _uploads.update { it + items }
    }

    private fun startEligibleUploads() {
        var next = nextAdmissibleUpload()
        while (next != null) {
            val item = next
            updateItem(item.id) { copy(state = DriveUploadState.Uploading()) }
            workers[item.id] = scope.launch { runUpload(item) }
            next = nextAdmissibleUpload()
        }
    }

    /**
     * The queue is strictly FIFO, so only the oldest queued upload is ever considered: a large file at the
     * head holds back the smaller files behind it instead of being skipped, which keeps the order the user
     * sees stable. Files above [LARGE_FILE_THRESHOLD_BYTES] additionally run exclusively, occupying the
     * whole pipeline and only starting once every running upload has drained.
     */
    private fun nextAdmissibleUpload(): DriveUploadItem? {
        val items = _uploads.value
        val running = items.filter { it.state is DriveUploadState.Uploading }
        val next = items.firstOrNull { it.state is DriveUploadState.Queued }
        val hasSlot = when {
            running.any { it.isLarge } -> false
            next == null -> false
            next.isLarge -> running.isEmpty()
            else -> running.size < MAX_CONCURRENT_UPLOADS
        }
        return next?.takeIf { hasSlot }
    }

    private suspend fun runUpload(item: DriveUploadItem) {
        // A node uuid from an earlier attempt lets the manager resume that draft. If it no longer knows the
        // node, retryUpload() would silently do nothing, so a fresh draft is created instead.
        val knownNode = item.nodeUuid?.takeIf { uploadManager.isUploading(it) }
        val nodeUuid = when {
            knownNode != null -> knownNode.also { uploadManager.retryUpload(it) }
            else -> createDraftNode(item) ?: return
        }
        observeUpload(item.id, nodeUuid)
    }

    private suspend fun createDraftNode(item: DriveUploadItem): String? {
        val request = item.request
        val destNodePath = "${request.destinationFolderPath}/${request.fileName}"
        return uploadManager.upload(request.localPath, request.sizeBytes, destNodePath).nullableFold(
            {
                commands.trySend(Command.Finished(item.id, DriveUploadState.Failed))
                null
            },
            { node ->
                commands.trySend(Command.NodeCreated(item.id, node.uuid))
                node.uuid
            }
        )
    }

    private suspend fun observeUpload(itemId: String, nodeUuid: String) = coroutineScope {
        val events = uploadManager.observeUpload(nodeUuid) ?: run {
            // The manager drops an upload from its registry as soon as it succeeds, so a missing flow at
            // this point means the transfer already finished.
            commands.trySend(Command.Finished(itemId, DriveUploadState.Completed))
            return@coroutineScope
        }
        val collector = launch { collectEvents(itemId, events) }
        yield()
        reportOutcomeReachedBeforeSubscribing(itemId, nodeUuid)
        collector.join()
    }

    private suspend fun collectEvents(itemId: String, events: Flow<CellUploadEvent>) {
        events.collect { event ->
            when (event) {
                is CellUploadEvent.UploadProgress -> commands.trySend(Command.Progress(itemId, event.progress))
                CellUploadEvent.UploadCompleted -> commands.trySend(Command.Finished(itemId, DriveUploadState.Completed))
                CellUploadEvent.UploadError -> commands.trySend(Command.Finished(itemId, DriveUploadState.Failed))
                // The scheduler marks the item cancelled itself, since it is the only thing that cancels.
                CellUploadEvent.UploadCancelled -> Unit
            }
        }
    }

    /**
     * Recovers an outcome that the transfer may have reached before [collectEvents] subscribed, since the
     * manager starts uploading before handing out its event flow and that flow replays nothing. Without
     * this, a fast failure would leave the item stuck as `Uploading` and its slot occupied forever.
     */
    private fun reportOutcomeReachedBeforeSubscribing(itemId: String, nodeUuid: String) {
        val info = uploadManager.getUploadInfo(nodeUuid)
        when {
            info == null -> commands.trySend(Command.Finished(itemId, DriveUploadState.Completed))
            info.uploadFailed -> commands.trySend(Command.Finished(itemId, DriveUploadState.Failed))
            else -> Unit
        }
    }

    private suspend fun cancelItem(id: String) {
        val item = _uploads.value.firstOrNull { it.id == id } ?: return
        when (item.state) {
            DriveUploadState.Queued -> Unit
            // Aborts the real transfer. The worker is still draining events here, which is what lets the
            // manager's cancellation event through; stopping it first would hang this call.
            is DriveUploadState.Uploading -> item.nodeUuid?.let { uploadManager.cancelUpload(it) }
            else -> return
        }
        stopWorker(id)
        updateItem(id) { copy(state = DriveUploadState.Cancelled) }
    }

    private suspend fun cancelAllItems() {
        _uploads.value
            .filter { it.state is DriveUploadState.Queued || it.state is DriveUploadState.Uploading }
            .forEach { cancelItem(it.id) }
    }

    private fun requeueFailed(id: String) {
        updateItem(id) { if (state is DriveUploadState.Failed) copy(state = DriveUploadState.Queued) else this }
    }

    private fun requeueAllFailed() {
        _uploads.value.filter { it.state is DriveUploadState.Failed }.forEach { requeueFailed(it.id) }
    }

    private fun updateProgress(id: String, progress: Float) {
        updateItem(id) {
            if (state is DriveUploadState.Uploading) copy(state = DriveUploadState.Uploading(progress)) else this
        }
    }

    private fun finishItem(id: String, outcome: DriveUploadState) {
        stopWorker(id)
        // Only a running upload can finish. This drops outcomes that race a cancellation, and the duplicate
        // reports that the recovery check above can produce.
        updateItem(id) { if (state is DriveUploadState.Uploading) copy(state = outcome) else this }
    }

    private fun stopWorker(id: String) {
        workers.remove(id)?.cancel()
    }

    private fun updateItem(id: String, block: DriveUploadItem.() -> DriveUploadItem) {
        _uploads.update { items -> items.map { if (it.id == id) block(it) else it } }
    }

    private val DriveUploadItem.isLarge: Boolean
        get() = sizeBytes > LARGE_FILE_THRESHOLD_BYTES

    private sealed interface Command {
        data class Enqueue(val requests: List<DriveUploadRequest>) : Command
        data class Cancel(val id: String) : Command
        data object CancelAll : Command
        data class Retry(val id: String) : Command
        data object RetryAllFailed : Command
        data class NodeCreated(val id: String, val nodeUuid: String) : Command
        data class Progress(val id: String, val progress: Float) : Command
        data class Finished(val id: String, val state: DriveUploadState) : Command
    }
}

private const val MAX_CONCURRENT_UPLOADS = 3

// Matches the size at which the S3 client switches to multipart upload.
private const val LARGE_FILE_THRESHOLD_BYTES = 100 * 1024 * 1024L
