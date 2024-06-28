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
package com.wire.android.gradle.version

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
        return if (localDateTime <= V2_DATE_OFFSET) {
            val duration = Duration.between(V1_DATE_OFFSET, localDateTime)
            (duration.seconds / V1_SECONDS_PER_BUMP).toInt()
        } else { // Use V2
            val duration = Duration.between(V2_DATE_OFFSET, localDateTime)
            V2_VERSION_CODE_OFFSET + (duration.toMinutes() / V2_MINUTES_PER_BUMP).toInt()
        }
    }

    companion object {
        // This is Google Play Max Version Code allowed
        // https://developer.android.com/studio/publish/versioning
        const val MAX_VERSION_CODE_ALLOWED = 2_100_000_000

        // The time-based versioning on the current Android project subtracts from this date to start the count
        private val V1_DATE_OFFSET = LocalDateTime.of(2021, 4, 21, 1, 0)
        private const val V1_SECONDS_PER_BUMP = 10

        // V2 starts at 100 million and 1 thousand
        private const val V2_VERSION_CODE_OFFSET = 100_001_000

        // From this date onwards, we bump every 5 min instead of every 10 seconds
        private val V2_DATE_OFFSET = LocalDateTime.of(2024, 6, 21, 0, 0)
        private const val V2_MINUTES_PER_BUMP = 5
    }
}
