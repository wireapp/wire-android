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
package com.wire.android.feature.privacy.session

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import com.wire.android.appLogger
import com.wire.android.di.ApplicationContext
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.id.ConversationId
import dev.zacsweers.metro.Inject

/**
 * Purges decrypted artefacts when a highly-sensitive conversation is locked.
 *
 * Honest scope (documented limits):
 * - Coil memory + disk caches are cleared so decrypted media is dropped (Coil keys are content
 *   hashes, not conversation-scoped, so we clear globally rather than per conversation — acceptable
 *   since lock events are infrequent).
 * - Decrypted message text lives only in the conversation screen's state, which the UI stops
 *   composing while locked; it then becomes GC-eligible. JVM strings cannot be force-zeroed — this
 *   is a platform limitation, not a leak (the encrypted source remains in SQLCipher).
 * - The clipboard is cleared to avoid leaking a just-copied secret.
 * - App-switcher previews are handled separately by FLAG_SECURE.
 */
@Inject
class SecureSessionCleaner(
    @ApplicationContext private val context: Context,
    private val imageLoader: WireSessionImageLoader,
) {
    @Suppress("TooGenericExceptionCaught")
    fun purge(conversationId: ConversationId) {
        appLogger.i("$TAG purging decrypted caches on lock")
        try {
            imageLoader.clearCaches()
        } catch (e: Exception) {
            appLogger.w("$TAG failed to clear image caches", e)
        }
        clearClipboard()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun clearClipboard() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip()
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        } catch (e: Exception) {
            appLogger.w("$TAG failed to clear clipboard", e)
        }
    }

    companion object {
        private const val TAG = "SecureSessionCleaner"
    }
}
