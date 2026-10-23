/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package service.models

import user.utils.ClientUser
import java.time.Instant
import java.time.ZoneId

data class Meeting(
    val id: QualifiedID,
    val conversationId: QualifiedID,
    val creator: ClientUser,
    val title: String,
    val start: Instant,
    val end: Instant,
    val zone: ZoneId,
    val recurring: Boolean,
)
