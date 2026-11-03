/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.conversations.messagelist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.wire.android.ui.markdown.InlineActionFocusRegistry

/**
 * Restores the keyboard-focused link or mention after its dialog or profile is closed,
 * without changing normal Tab order between messages and their inline targets.
 *
 * Obtain this with [rememberMessageContentFocusRestoration]. While each message is composed,
 * [register] its container's focus requester and content hash, provide [forMessage] as its
 * descendant text's inline-action focus registry, and [unregister] the message on disposal.
 * The registry records the activated occurrence before navigation; [hasPendingRestoration]
 * lets the message list preserve navigation mode while that action is open.
 *
 * The remember helper retains the pending target across destination recreation and calls
 * [restore] when the conversation resumes with window focus. If the content changed or the
 * inline target is unavailable, focus returns to the message container; if the message is
 * unavailable too, it returns to the supplied fallback, normally the message list.
 */
internal class MessageContentFocusRestoration(
    private val pendingMessageId: MutableState<String?>,
    private val pendingContentHash: MutableState<Int?>,
    private val pendingGroupId: MutableState<String?>,
    private val pendingTargetIndex: MutableState<Int>,
) {
    private data class Target(val requester: FocusRequester, val contentHash: Int)

    private val targets = mutableMapOf<String, Target>()
    private val inlineGroups = mutableMapOf<String, (Int) -> Boolean>()
    val hasPendingRestoration: Boolean get() = pendingMessageId.value != null

    fun register(messageId: String, requester: FocusRequester, contentHash: Int) {
        targets[messageId] = Target(requester, contentHash)
    }

    fun unregister(messageId: String) {
        targets.remove(messageId)
    }

    fun forMessage(messageId: String): InlineActionFocusRegistry = object : InlineActionFocusRegistry {
        override fun register(groupId: String, restore: (Int) -> Boolean) {
            inlineGroups[groupId] = restore
        }

        override fun unregister(groupId: String) {
            inlineGroups.remove(groupId)
        }

        override fun save(groupId: String, targetIndex: Int) {
            val target = targets[messageId] ?: return
            pendingMessageId.value = messageId
            pendingContentHash.value = target.contentHash
            pendingGroupId.value = groupId
            pendingTargetIndex.value = targetIndex
        }
    }

    fun restore(fallback: FocusRequester) {
        val target = targets[pendingMessageId.value]
        if (target == null) {
            fallback.requestFocus()
        } else if (
            target.contentHash != pendingContentHash.value ||
            inlineGroups[pendingGroupId.value]?.invoke(pendingTargetIndex.value) != true
        ) {
            // If the text or its inline targets changed while away, fall back to the message.
            target.requester.requestFocus()
        }
        pendingMessageId.value = null
        pendingContentHash.value = null
        pendingGroupId.value = null
    }
}

@Composable
internal fun rememberMessageContentFocusRestoration(
    fallback: FocusRequester,
    onRestore: () -> Unit,
): MessageContentFocusRestoration {
    val pendingMessageId = rememberSaveable { mutableStateOf<String?>(null) }
    val pendingContentHash = rememberSaveable { mutableStateOf<Int?>(null) }
    val pendingGroupId = rememberSaveable { mutableStateOf<String?>(null) }
    val pendingTargetIndex = rememberSaveable { mutableStateOf(0) }
    val state = remember {
        MessageContentFocusRestoration(pendingMessageId, pendingContentHash, pendingGroupId, pendingTargetIndex)
    }
    val currentOnRestore by rememberUpdatedState(onRestore)
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val isWindowFocused = LocalWindowInfo.current.isWindowFocused
    // Do not key this effect on the pending action: it must run on RETURN from the other
    // window/destination, never immediately after activation while the action is opening.
    LaunchedEffect(isWindowFocused, lifecycleState) {
        if (isWindowFocused && lifecycleState == Lifecycle.State.RESUMED && state.hasPendingRestoration) {
            currentOnRestore()
            withFrameNanos { }
            state.restore(fallback)
        }
    }
    return state
}
