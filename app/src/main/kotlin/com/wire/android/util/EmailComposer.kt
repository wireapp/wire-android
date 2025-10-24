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

package com.wire.android.util

import android.os.Build
import java.util.Date

sealed interface EmailComposer {

    companion object {

        // TODO(localization): localize if needed
        fun reportBugEmailTemplate(deviceHash: String? = "unavailable", commitHash: String? = "unavailable"): String = """
        ${emailDebugHeader(deviceHash, commitHash)}

        Please fill in the following

        - Date & Time of when the issue occurred:


        - What happened:


        - Steps to reproduce (if relevant):
        
        """.trimIndent()

        // TODO(localization): localize if needed
        fun giveFeedbackEmailTemplate(deviceHash: String? = "unavailable", commitHash: String? = "unavailable"): String = """
        ${emailDebugHeader(deviceHash, commitHash)}

        Thank you for taking interest from the Wire Team!

        """.trimIndent()

        private fun emailDebugHeader(deviceHash: String?, commitHash: String? = "unavailable"): String = """
        --- DO NOT EDIT---
        App Version: ${AppNameUtil.createAppName()}
        Device Hash: $deviceHash
        Device: ${Build.MANUFACTURER} - ${Build.MODEL}
        SDK: ${Build.VERSION.RELEASE}
        Date: ${Date()}
        Commit Hash: $commitHash
        ------------------
        """
    }
}
