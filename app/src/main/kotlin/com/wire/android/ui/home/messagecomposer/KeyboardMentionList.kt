/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.Contact
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.home.conversations.mention.MemberItemToMention
import com.wire.android.ui.theme.wireColorScheme

@Composable
internal fun KeyboardMentionList(
    membersToMention: List<Contact>,
    searchQuery: String,
    onMentionPicked: (Contact) -> Unit,
    onDismissRequest: () -> Unit,
    firstItemFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    reverseLayout: Boolean = false,
    showEveryoneMention: Boolean = false,
    onEveryoneMentionPicked: () -> Unit = {},
) {
    val entries = if (showEveryoneMention) listOf(null) + membersToMention else membersToMention
    val memberKeys = membersToMention.map { it.id to it.domain }
    val focusRequesters = remember(memberKeys, showEveryoneMention, firstItemFocusRequester) {
        listOf(firstItemFocusRequester) + List((entries.size - 1).coerceAtLeast(0)) { FocusRequester() }
    }
    LazyColumn(
        modifier = modifier.focusGroup(),
        reverseLayout = reverseLayout,
    ) {
        itemsIndexed(
            items = entries,
            key = { _, member -> member?.let { "${it.id}@${it.domain}" } ?: "everyone" },
        ) { index, member ->
            KeyboardMentionItem(
                member = member,
                searchQuery = searchQuery,
                index = index,
                focusRequesters = focusRequesters,
                onMentionPicked = onMentionPicked,
                onEveryoneMentionPicked = onEveryoneMentionPicked,
                onDismissRequest = onDismissRequest,
                reverseLayout = reverseLayout,
            )
        }
    }
}

@Composable
private fun KeyboardMentionItem(
    member: Contact?,
    searchQuery: String,
    index: Int,
    focusRequesters: List<FocusRequester>,
    onMentionPicked: (Contact) -> Unit,
    onEveryoneMentionPicked: () -> Unit,
    onDismissRequest: () -> Unit,
    reverseLayout: Boolean,
) {
    var isFocused by remember { mutableStateOf(false) }
    val everyoneDescription = stringResource(R.string.mention_everyone_description)
    val pickMention = { if (member == null) onEveryoneMentionPicked() else onMentionPicked(member) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequesters[index])
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                handleMentionKeyEvent(
                    event = event,
                    context = MentionKeyContext(
                        index = index,
                        focusRequesters = focusRequesters,
                        onMentionPicked = pickMention,
                        onDismissRequest = onDismissRequest,
                        reverseLayout = reverseLayout,
                    ),
                )
            }
            .clearAndSetSemantics {
                contentDescription = member?.let {
                    listOf(it.name, it.label).filter { part -> part.isNotBlank() }.joinToString(", ")
                } ?: "@all, $everyoneDescription"
                role = Role.Button
                onClick {
                    pickMention()
                    true
                }
            }
            .focusable(),
        contentAlignment = Alignment.CenterStart,
    ) {
        val itemModifier = Modifier.fillMaxWidth().focusProperties { canFocus = false }
        val backgroundColor = if (isFocused) MaterialTheme.wireColorScheme.primaryVariant else colorsScheme().surface
        if (member == null) {
            EveryoneItemToMention(
                searchQuery = searchQuery,
                clickable = Clickable { pickMention() },
                modifier = itemModifier,
                backgroundColor = backgroundColor,
            )
        } else {
            MemberItemToMention(
                avatarData = member.avatarData,
                name = member.name,
                label = member.label,
                membership = member.membership,
                clickable = Clickable { pickMention() },
                searchQuery = searchQuery,
                modifier = itemModifier,
                backgroundColor = backgroundColor,
            )
        }
    }
}

private fun handleMentionKeyEvent(
    event: KeyEvent,
    context: MentionKeyContext,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val action = mentionKeyAction(event.key, event.isShiftPressed, context.reverseLayout)
    return when (action) {
        MentionKeyAction.Pick -> {
            context.onMentionPicked()
            true
        }

        MentionKeyAction.Dismiss -> {
            context.onDismissRequest()
            true
        }

        MentionKeyAction.Previous, MentionKeyAction.Next -> {
            val move = if (action == MentionKeyAction.Previous) {
                MentionFocusMove.Previous
            } else {
                MentionFocusMove.Next
            }
            context.focusRequesters[
                mentionFocusTargetIndex(context.index, context.focusRequesters.size, move)
            ].requestFocus()
            true
        }

        null -> false
    }
}

private data class MentionKeyContext(
    val index: Int,
    val focusRequesters: List<FocusRequester>,
    val onMentionPicked: () -> Unit,
    val onDismissRequest: () -> Unit,
    val reverseLayout: Boolean,
)

internal enum class MentionFocusMove { Previous, Next }

internal enum class MentionKeyAction { Pick, Dismiss, Previous, Next }

internal fun mentionKeyAction(
    key: Key,
    isShiftPressed: Boolean,
    reverseLayout: Boolean = false,
): MentionKeyAction? = when (key) {
    Key.Enter, Key.NumPadEnter, Key.Spacebar -> MentionKeyAction.Pick
    Key.Escape -> MentionKeyAction.Dismiss
    Key.DirectionUp -> if (reverseLayout) MentionKeyAction.Next else MentionKeyAction.Previous
    Key.DirectionDown -> if (reverseLayout) MentionKeyAction.Previous else MentionKeyAction.Next
    Key.Tab -> if (isShiftPressed) MentionKeyAction.Previous else MentionKeyAction.Next
    else -> null
}

internal fun mentionFocusTargetIndex(currentIndex: Int, itemCount: Int, move: MentionFocusMove): Int = when (move) {
    MentionFocusMove.Previous -> (currentIndex - 1 + itemCount) % itemCount
    MentionFocusMove.Next -> (currentIndex + 1) % itemCount
}
