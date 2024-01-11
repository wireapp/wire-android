/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.util.time

import java.text.DateFormat
import java.time.Instant
import java.util.Date
import javax.inject.Inject

class ISOFormatter @Inject constructor() {

    fun fromISO8601ToTimeFormat(utcISO: String): String {
        val formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT)
        val date = Date.from(Instant.parse(utcISO))

        return formatter.format(date)
    }

}
