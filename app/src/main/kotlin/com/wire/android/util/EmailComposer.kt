package com.wire.android.util

import android.os.Build
import com.wire.android.BuildConfig
import java.util.Date

sealed interface EmailComposer {

    companion object {

        // TODO(localization): localize if needed
        fun reportBugEmailTemplate(deviceHash: String? = "unavailable"): String = """
        ${emailDebugHeader(deviceHash)}

        Please fill in the following

        - Date & Time of when the issue occurred:


        - What happened:


        - Steps to reproduce (if relevant):
        
        """.trimIndent()

        // TODO(localization): localize if needed
        fun giveFeedbackEmailTemplate(deviceHash: String? = "unavailable"): String = """
        ${emailDebugHeader(deviceHash)}

        Thank you for taking interest from the Wire Team!

        """.trimIndent()

        private fun emailDebugHeader(deviceHash: String?): String = """
        --- DO NOT EDIT---
        App Version: ${BuildConfig.VERSION_NAME}
        Device Hash: $deviceHash
        Device: ${Build.MANUFACTURER} - ${Build.MODEL}
        SDK: ${Build.VERSION.RELEASE}
        Date: ${Date()}
        ------------------
        """
    }
}
