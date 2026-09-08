/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.calling

import android.content.Intent
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId

/** Immutable input consumed by the call Activity composition root. */
data class CallActivityRequest(
    val userId: UserId,
    val conversationId: ConversationId,
    val screen: CallActivityScreen,
    val shouldAnswerCall: Boolean,
)

sealed interface CallActivityScreen {
    data class Starting(val type: StartingCallScreenType) : CallActivityScreen
    data object Ongoing : CallActivityScreen
}

enum class CallActivityDestination {
    STARTING,
    ONGOING,
}

/**
 * The only parser for call routing intents.
 *
 * Both initial launch and [android.app.Activity.onNewIntent] go through this boundary, so a
 * malformed replacement intent can never partially update an existing call session.
 */
internal fun Intent.toCallActivityRequest(
    destination: CallActivityDestination,
    qualifiedIdMapper: QualifiedIdMapper,
): CallActivityRequest? {
    val userId = getStringExtra(CallActivity.EXTRA_USER_ID)
    val conversationId = getStringExtra(CallActivity.EXTRA_CONVERSATION_ID)
    val screen = when (destination) {
        CallActivityDestination.STARTING -> getStringExtra(CallActivity.EXTRA_SCREEN_TYPE)
            ?.let(StartingCallScreenType::byName)
            ?.let(CallActivityScreen::Starting)

        CallActivityDestination.ONGOING -> CallActivityScreen.Ongoing
    }

    return if (userId == null || conversationId == null || screen == null) {
        null
    } else {
        runCatching {
            CallActivityRequest(
                userId = qualifiedIdMapper.fromStringToQualifiedID(userId),
                conversationId = qualifiedIdMapper.fromStringToQualifiedID(conversationId),
                screen = screen,
                shouldAnswerCall = getBooleanExtra(CallActivity.EXTRA_SHOULD_ANSWER_CALL, false),
            )
        }.getOrNull()
    }
}
