package com.wire.android.gradle.version
/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

import java.time.Duration
import java.time.LocalDateTime

/**
 * Creates a time-based, auto-incrementing Integer build version.
 * It takes into consideration the epoch seconds, incrementing the build by one every ten seconds.
 * In order to maximize the range, an offset was added. So instead of starting the count on 1970-01-01, it starts on 2021-04-21.
 * This has been built to match the current Groovy implementation:
 * https://github.com/wireapp/wire-android/blob/594497477325d77c1d203dbcaab79fb14b511530/app/build.gradle#L467
 */
class Versionizer(private val localDateTime: LocalDateTime = LocalDateTime.now()) {

    val versionCode = generateVersionCode()

    private fun generateVersionCode(): Int {
        val duration = Duration.between(DATE_OFFSET, localDateTime)
        return (duration.seconds / 10).toInt()
    }

    companion object {
        //This is Google Play Max Version Code allowed
        //https://developer.android.com/studio/publish/versioning
        const val MAX_VERSION_CODE_ALLOWED = 2100000000

        // The time-based versioning on the current Android project subtracts from this date to start the count
        private val DATE_OFFSET = LocalDateTime.of(2021, 4, 21, 1, 0)
    }
}
