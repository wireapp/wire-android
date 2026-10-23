@file:Suppress("PackageNaming")
/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package backendUtils.meeting

import android.net.Uri
import backendUtils.BackendClient
import backendUtils.auth.defaultheaders
import backendUtils.auth.getAuthToken
import kotlinx.coroutines.runBlocking
import network.NetworkBackendClient
import network.RequestOptions
import org.json.JSONObject
import service.models.Meeting
import service.models.QualifiedID
import user.utils.ClientUser
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@Suppress("LongParameterList")
fun BackendClient.createMeeting(
    creator: ClientUser,
    title: String,
    start: Instant,
    end: Instant = start.plus(Duration.ofMinutes(DEFAULT_MEETING_DURATION_MINUTES)),
    zone: ZoneId = ZoneId.of("UTC"),
    recurring: Boolean = false,
): Meeting {
    val body = meetingBody(title, start, end, zone, recurring)
    val response = JSONObject(sendMeetingRequest(creator, "meetings", "POST", body))
    return Meeting(
        id = QualifiedID.fromJSON(response.getJSONObject("qualified_id")),
        conversationId = QualifiedID.fromJSON(response.getJSONObject("qualified_conversation")),
        creator = creator,
        title = response.getString("title"),
        start = Instant.parse(response.getString("start_time")),
        end = Instant.parse(response.getString("end_time")),
        zone = zone,
        recurring = recurring,
    )
}

fun BackendClient.updateMeeting(meeting: Meeting, title: String, start: Instant, end: Instant): Meeting {
    sendMeetingRequest(meeting.creator, meetingPath(meeting), "PUT", meetingBody(title, start, end, meeting.zone, meeting.recurring))
    return meeting.copy(title = title, start = start, end = end)
}

fun BackendClient.getMeeting(meeting: Meeting, asUser: ClientUser): JSONObject =
    JSONObject(sendMeetingRequest(asUser, meetingPath(meeting), "GET"))

fun BackendClient.deleteMeeting(meeting: Meeting) {
    sendMeetingRequest(meeting.creator, meetingPath(meeting), "DELETE")
}

private fun meetingBody(title: String, start: Instant, end: Instant, zone: ZoneId, recurring: Boolean) = JSONObject().apply {
    put("title", title)
    put("start_time", start.toString())
    put("end_time", end.toString())
    put("tzid", zone.id)
    // A bounded recurring series leaves one occurrence in this fixture's list and still shows its recurrence label.
    val recurrence = if (recurring) {
        JSONObject().put("frequency", "daily").put("interval", 1).put("until", end.toString())
    } else {
        JSONObject.NULL
    }
    put("recurrence", recurrence)
}

private fun meetingPath(meeting: Meeting) = "meetings/${Uri.encode(meeting.id.domain)}/${Uri.encode(meeting.id.id)}"
private fun conversationPath(meeting: Meeting) =
    "conversations/${Uri.encode(meeting.conversationId.domain)}/${Uri.encode(meeting.conversationId.id)}"

private fun BackendClient.sendMeetingRequest(
    user: ClientUser,
    path: String,
    method: String,
    body: JSONObject? = null,
): String = runBlocking {
    val token = requireNotNull(getAuthToken(user))
    NetworkBackendClient.sendJsonRequestWithCookies(
        url = URI(path.composePublicApiUrl()).toURL(),
        method = method,
        headers = defaultheaders,
        body = body?.toString(),
        options = RequestOptions(accessToken = token)
    ).body
}

fun BackendClient.getMeetingConversation(meeting: Meeting): JSONObject =
    JSONObject(sendMeetingRequest(meeting.creator, conversationPath(meeting), "GET"))

private const val DEFAULT_MEETING_DURATION_MINUTES = 30L
