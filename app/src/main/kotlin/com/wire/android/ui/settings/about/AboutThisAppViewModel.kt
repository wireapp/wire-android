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
package com.wire.android.ui.settings.about

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.util.getGitBuildId
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AboutThisAppViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    var state by mutableStateOf(
        AboutThisAppState(
            appName = createAppName()
        )
    )

    init {
        setGitHash()
    }

    private fun setGitHash() {
        viewModelScope.launch {
            val gitBuildId = context.getGitBuildId()
            state = state.copy(
                commitish = gitBuildId
            )
        }
    }

    private fun createAppName(): String {
        return "${BuildConfig.VERSION_NAME}-${leastSignificantVersionCode()}-${BuildConfig.FLAVOR}"
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
    private fun leastSignificantVersionCode(): Int {
        return BuildConfig.VERSION_CODE % 100_000
    }
}
