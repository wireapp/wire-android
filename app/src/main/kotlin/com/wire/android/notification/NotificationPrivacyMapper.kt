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
package com.wire.android.notification

import android.content.Context
import com.wire.android.R
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.ApplicationContext
import com.wire.android.feature.privacy.data.ConversationPrivacyStoreProvider
import com.wire.android.feature.privacy.model.EffectivePrivacyLevel
import com.wire.android.feature.privacy.model.PrivacyResolver
import com.wire.android.feature.privacy.panic.PanicModeManager
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first

/**
 * Redacts notification content according to each conversation's effective privacy level. This is the
 * single OS-facing choke point where message content reaches the notification tray, so applying the
 * policy here guarantees no sensitive content escapes via notifications.
 *
 * - SENSITIVE: keep the conversation name, drop the body and sender → "New message".
 * - HIGHLY_SENSITIVE: drop name, sender and body → "New Secure Message"; reply disabled.
 * - Panic Mode escalation is applied via [PrivacyResolver].
 * - A global "hide content for private chats" floor forces every non-normal chat to the secure style.
 *
 * Note: it reads the privacy store for the *notification's* user (not the current session), so it is
 * correct for multi-account and when no session is active. Panic Mode is a global device-level switch.
 */
@Inject
class NotificationPrivacyMapper(
    @ApplicationContext private val context: Context,
    private val storeProvider: ConversationPrivacyStoreProvider,
    private val panicModeManager: PanicModeManager,
    private val globalDataStore: GlobalDataStore,
) {
    /** Captured once per notification batch (the per-notification code path is not suspend). */
    data class Snapshot(
        val effectiveByConversation: Map<ConversationId, EffectivePrivacyLevel>,
        val hideAllPrivateContent: Boolean,
    )

    suspend fun snapshot(userId: UserId): Snapshot {
        val panicActive = panicModeManager.isActive
        val effective = storeProvider.getOrCreate(userId).currentMap()
            .mapValues { (_, settings) -> PrivacyResolver.resolve(settings.level, panicActive, settings.panicProtected) }
        return Snapshot(
            effectiveByConversation = effective,
            hideAllPrivateContent = globalDataStore.hideNotificationContentForPrivateChats().first(),
        )
    }

    /** True when this conversation's content must not be revealed (used to suppress edit/update leaks). */
    fun isRedacted(conversationIdString: String, snapshot: Snapshot): Boolean =
        effectiveLevel(conversationIdString, snapshot) != EffectivePrivacyLevel.NORMAL

    fun redact(conversation: NotificationConversation, snapshot: Snapshot): NotificationConversation =
        when (effectiveLevel(conversation.id, snapshot)) {
            EffectivePrivacyLevel.NORMAL ->
                conversation

            EffectivePrivacyLevel.SENSITIVE ->
                // Sensitive (no panic): keep the name, drop the body → "New message".
                conversation.copy(
                    messages = listOf(placeholder(conversation, R.string.notification_sensitive_new_message)),
                )

            EffectivePrivacyLevel.SENSITIVE_PANIC,
            EffectivePrivacyLevel.HIGHLY_SENSITIVE,
            EffectivePrivacyLevel.HIGHLY_SENSITIVE_PANIC ->
                // Highly-sensitive, or anything escalated by Panic Mode: reveal nothing → "New Secure Message".
                conversation.copy(
                    name = null,
                    isOneToOneConversation = true, // suppress group title rendering
                    isReplyAllowed = false, // never reply from an unauthenticated context
                    messages = listOf(placeholder(conversation, R.string.notification_secure_new_message)),
                )
        }

    private fun effectiveLevel(conversationIdString: String, snapshot: Snapshot): EffectivePrivacyLevel {
        val id = conversationIdString.toConversationIdOrNull() ?: return EffectivePrivacyLevel.NORMAL
        val base = snapshot.effectiveByConversation[id] ?: EffectivePrivacyLevel.NORMAL
        // Global floor: hide all content for non-normal chats when the user enabled it.
        return if (snapshot.hideAllPrivateContent && base != EffectivePrivacyLevel.NORMAL) {
            EffectivePrivacyLevel.HIGHLY_SENSITIVE
        } else {
            base
        }
    }

    private fun placeholder(conversation: NotificationConversation, textResId: Int): NotificationMessage =
        NotificationMessage.Text(
            messageId = conversation.messages.lastOrNull()?.messageId ?: "redacted_${conversation.id}",
            author = NotificationMessageAuthor(""),
            time = conversation.lastMessageTime,
            text = context.getString(textResId),
            isQuotingSelfUser = false,
        )

    private fun String.toConversationIdOrNull(): ConversationId? {
        val idx = lastIndexOf('@')
        if (idx <= 0 || idx == length - 1) return null
        return ConversationId(substring(0, idx), substring(idx + 1))
    }
}
