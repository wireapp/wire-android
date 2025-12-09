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
import com.wire.android.gradle.version.Versionizer
import java.io.File

object AndroidSdk {
    const val min = 26
    const val compile = 35
    const val target = compile
}

object AndroidApp {
    const val id = "com.wire.android"
    const val versionName = "4.18.0"
    val versionCode by lazy {
        Versionizer(_rootDir).versionCode
    }

    private lateinit var _rootDir: File
    fun setRootDir(rootDir: File) {
        this._rootDir = rootDir
    }

    /**
     * The last 5 digits of the VersionCode. From 0 to 99_999.
     * It's an [Int], so it can be less than 5 digits when doing [toString], of course.
     * Considering versionCode bumps every 5min, these are
     * 288 per day
     * 8640 per month
     * 51840 per semester
     * 103_680 per year. ~99_999
     *
     * So it takes almost a whole year until it rotates back.
     * It's very unlikely that two APKs with the same version (_e.g._ 4.8.0)
     * will have the same [leastSignificantVersionCode],
     * unless they are build almost one year apart.
     */
    @Suppress("MagicNumber")
    val leastSignificantVersionCode by lazy {
        versionCode % 100_000
    }
}
