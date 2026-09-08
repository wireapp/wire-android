/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.feature.meetings.ui.create

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import com.wire.kalium.logic.data.id.MeetingId
import kotlinx.serialization.Serializable

@Serializable
enum class NewMeetingRouteType {
    MEET_NOW,
    SCHEDULE,
    EDIT,
}

@Serializable
sealed interface NewMeetingRoute : SessionRoute {
    override val flowId: String
}

@Serializable
data class NewMeetingDetailsRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    val type: NewMeetingRouteType,
    val meetingId: MeetingId? = null,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : NewMeetingRoute {
    override val routeId: String = ROUTE_ID

    init {
        require(flowId.isNotBlank())
    }

    companion object {
        const val ROUTE_ID = "meetings/new_meeting_screen"

        fun start(
            sessionId: WireSessionId,
            type: NewMeetingRouteType,
            meetingId: MeetingId? = null,
            entryId: WireNavEntryId = WireNavEntryId.random(),
        ) = NewMeetingDetailsRoute(
            sessionId = sessionId,
            flowId = "new-meeting:${entryId.value}",
            type = type,
            meetingId = meetingId,
            entryId = entryId,
        )

        fun start(
            sessionId: WireSessionId,
            type: NewMeetingType,
            entryId: WireNavEntryId = WireNavEntryId.random(),
        ) = start(
            sessionId = sessionId,
            type = type.toNavigation3Type(),
            meetingId = (type as? NewMeetingType.Edit)?.id,
            entryId = entryId,
        )
    }
}

@Serializable
data class NewMeetingParticipantsRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    val type: NewMeetingRouteType,
    val meetingId: MeetingId? = null,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : NewMeetingRoute {
    override val routeId: String = ROUTE_ID

    init {
        require(flowId.isNotBlank())
    }

    companion object {
        const val ROUTE_ID = "meetings/new_meeting_participants_screen"
    }
}

fun NewMeetingType.toNavigation3Type(): NewMeetingRouteType = when (this) {
    NewMeetingType.MeetNow -> NewMeetingRouteType.MEET_NOW
    NewMeetingType.Schedule -> NewMeetingRouteType.SCHEDULE
    is NewMeetingType.Edit -> NewMeetingRouteType.EDIT
}

internal fun NewMeetingRouteType.toLegacyType(meetingId: MeetingId? = null): NewMeetingType = when (this) {
    NewMeetingRouteType.MEET_NOW -> NewMeetingType.MeetNow
    NewMeetingRouteType.SCHEDULE -> NewMeetingType.Schedule
    NewMeetingRouteType.EDIT -> NewMeetingType.Edit(requireNotNull(meetingId))
}
