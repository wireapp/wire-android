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

import com.wire.kalium.logic.data.id.MeetingId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class MeetingsNavigation3Test {

    @Test
    fun `given legacy meeting types, when mapping to typed route values, then semantics are preserved`() {
        assertEquals(NewMeetingRouteType.MEET_NOW, NewMeetingType.MeetNow.toNavigation3Type())
        assertEquals(NewMeetingRouteType.SCHEDULE, NewMeetingType.Schedule.toNavigation3Type())
        assertEquals(NewMeetingRouteType.EDIT, NewMeetingType.Edit(MeetingId("meeting", "wire.test")).toNavigation3Type())
    }

    @Test
    fun `given edit meeting flow, when route is restored, then meeting identity survives`() {
        val meetingId = MeetingId("meeting", "wire.test")
        val route = NewMeetingDetailsRoute.start(Session, NewMeetingType.Edit(meetingId))

        val restored = Json.decodeFromString<NewMeetingDetailsRoute>(Json.encodeToString(route))

        assertEquals(NewMeetingRouteType.EDIT, restored.type)
        assertEquals(meetingId, restored.meetingId)
        assertEquals(NewMeetingType.Edit(meetingId), restored.type.toLegacyType(restored.meetingId))
    }

    @Test
    fun `given new meeting flow, when opening participants, then both entries share flow and session`() {
        val details = NewMeetingDetailsRoute.start(Session, NewMeetingRouteType.SCHEDULE)
        val participants = NewMeetingParticipantsRoute(Session, details.flowId, details.type)

        assertEquals(details.sessionId, participants.sessionId)
        assertEquals(details.flowId, participants.flowId)
        assertEquals(details.type, participants.type)
        assertNotEquals(details.entryId, participants.entryId)
    }

    @Test
    fun `given restored participants route, when serialized, then meeting type and flow ownership survive`() {
        val route = NewMeetingParticipantsRoute(
            Session,
            flowId = "new-meeting:test",
            type = NewMeetingRouteType.MEET_NOW,
        )

        val restored = Json.decodeFromString<NewMeetingParticipantsRoute>(
            Json.encodeToString(route)
        )

        assertEquals(route, restored)
    }

    private companion object {
        val Session = WireSessionId("user", "wire.test")
    }
}
