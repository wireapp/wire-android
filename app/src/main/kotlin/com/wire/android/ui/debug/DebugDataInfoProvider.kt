/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.debug

import android.content.Context
import com.wire.android.util.getDeviceIdString
import com.wire.android.util.getGitBuildId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface DebugDataInfoProvider {
    fun deviceId(): String?
    fun gitBuildId(): String
}

class AndroidDebugDataInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : DebugDataInfoProvider {
    override fun deviceId(): String? = context.getDeviceIdString()
    override fun gitBuildId(): String = context.getGitBuildId()
}
