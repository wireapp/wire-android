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
package com.wire.android.ui.home.conversations.model.messagetypes.multipart

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.PRIVATE
import com.wire.android.ui.common.multipart.MultipartAttachmentUi
import com.wire.android.ui.common.multipart.toUiModel
import com.wire.android.util.ExpiringMap
import com.wire.kalium.cells.domain.usecase.RefreshCellAssetStateUseCase
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.message.MessageAttachment
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class CellAssetRefreshHelper(
    private val refreshAsset: RefreshCellAssetStateUseCase,
    private val featureFlags: KaliumConfigs,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    private val currentTime: () -> Long = { System.currentTimeMillis() },
) {

    private companion object Companion {
        // Default refresh rate for not editable asset if URL expiration is missing
        val DEFAULT_CONTENT_URL_EXPIRY_MS = 1.hours.inWholeMilliseconds

        // Refresh rate for editable assets
        val EDITABLE_CONTENT_EXPIRY_MS = 30.seconds.inWholeMilliseconds
    }

  /**
   * Regular files.
   * - Refresh on first display.
   * - Refresh when content URL expires (unlikely).
   */
    @VisibleForTesting(otherwise = PRIVATE)
    val regularAssets = expiringMap(
        expirationMs = DEFAULT_CONTENT_URL_EXPIRY_MS,
        onExpired = { assetId ->
            coroutineScope.launch { refreshAsset(assetId) }
        },
    )

/**
 * Editable files. Could be updated frequently.
 * - Refresh on first display.
 * - Refresh every 30 sec to update preview if currently visible.
 */
    @VisibleForTesting(otherwise = PRIVATE)
    val visibleEditableAssets: ExpiringMap<String, Unit> = expiringMap(
        expirationMs = EDITABLE_CONTENT_EXPIRY_MS,
        onExpired = { assetId ->
            coroutineScope.launch { refreshAsset(assetId) }

            // Re-add to schedule next refresh
            visibleEditableAssets[assetId] = Unit
        }
    )

    private fun expiringMap(expirationMs: Long, onExpired: (String) -> Unit) = ExpiringMap<String, Unit>(
        scope = coroutineScope,
        expirationMs = expirationMs,
        delegate = mutableMapOf(),
        onEntryExpired = { key, _ -> onExpired(key) },
        currentTime = currentTime,
    )

    fun onAttachmentsVisible(attachments: List<MessageAttachment>) {
        attachments.forEach {
            onAttachmentVisible(it.toUiModel())
        }
    }

    fun onAttachmentsHidden(attachments: List<MessageAttachment>) {
        attachments.forEach {
            onAttachmentHidden(it.toUiModel())
        }
    }

    fun onAttachmentVisible(attachment: MultipartAttachmentUi) {
        if (attachment.isEditSupported && featureFlags.collaboraIntegration) {

            if (visibleEditableAssets.contains(attachment.uuid)) return

            visibleEditableAssets[attachment.uuid] = Unit

            coroutineScope.launch {
                refreshAsset(attachment.uuid)
            }
        } else {

            if (regularAssets.contains(attachment.uuid)) return

            if (attachment.contentUrlExpiresAt != null) {
                regularAssets.putWithExpireAt(attachment.uuid, Unit, attachment.contentUrlExpiresAt)
            } else {
                regularAssets[attachment.uuid] = Unit
            }

            coroutineScope.launch {
                refreshAsset(attachment.uuid)
                    .onSuccess { node ->
                        if (node.supportedEditors.isNotEmpty()) {
                            regularAssets.remove(attachment.uuid)
                            visibleEditableAssets[attachment.uuid] = Unit
                        }
                    }
            }
        }
    }

    fun onAttachmentHidden(attachment: MultipartAttachmentUi) {
        if (attachment.isEditSupported) {
            visibleEditableAssets.remove(attachment.uuid)
        }
    }

    fun refresh(uuid: String) {
        coroutineScope.launch {
            refreshAsset(uuid)
        }
    }

    fun close() {
        coroutineScope.cancel()
    }
}
