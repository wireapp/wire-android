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
 */
package com.wire.android.navigation

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import androidx.annotation.StringRes
import com.ramcosta.composedestinations.spec.Direction
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.util.EmailComposer
import com.wire.android.util.LogFileWriter
import com.wire.android.util.getDeviceIdString
import com.wire.android.util.getGitBuildId
import com.wire.android.util.getUrisOfFilesInDirectory
import com.wire.android.util.multipleFileSharingIntent
import com.wire.android.util.sha256

interface ExternalUriDirection : Direction {
    val uri: Uri
    override val route: String
        get() = uri.toString()
}

interface ExternalUriStringResDirection : Direction {
    @get:StringRes
    val uriStringRes: Int
    override val route: String
        get() = "android.resource://${BuildConfig.APPLICATION_ID}/$uriStringRes"

    fun getUri(resources: Resources): Uri = Uri.parse(resources.getString(uriStringRes))
}

interface IntentDirection : Direction {
    fun intent(context: Context): Intent
}

object SupportScreenDestination : ExternalUriDirection {
    override val uri: Uri
        get() = Uri.parse(BuildConfig.URL_SUPPORT)
}

object GiveFeedbackDestination : IntentDirection {
    override fun intent(context: Context): Intent {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(
            Intent.EXTRA_EMAIL,
            arrayOf("wire-newandroid-feedback@wearezeta.zendesk.com")
        )
        intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback - Wire Beta")
        intent.putExtra(
            Intent.EXTRA_TEXT,
            EmailComposer.giveFeedbackEmailTemplate(
                context.getDeviceIdString()?.sha256(),
                context.getGitBuildId()
            )
        )
        intent.selector = Intent(Intent.ACTION_SENDTO).setData(Uri.parse("mailto:"))
        return Intent.createChooser(intent, context.getString(R.string.send_feedback_choose_email))
    }

    override val route: String
        get() = "wire-intent:give-feedback"
}

object ReportBugDestination : IntentDirection {
    override fun intent(context: Context): Intent {
        val dir = LogFileWriter.logsDirectory(context)
        val logsUris = context.getUrisOfFilesInDirectory(dir)
        val intent = context.multipleFileSharingIntent(logsUris)
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("wire-newandroid@wearezeta.zendesk.com"))
        intent.putExtra(Intent.EXTRA_SUBJECT, "Bug Report - Wire Beta")
        intent.putExtra(
            Intent.EXTRA_TEXT,
            EmailComposer.reportBugEmailTemplate(
                context.getDeviceIdString()?.sha256(),
                context.getGitBuildId()
            )
        )
        intent.type = "message/rfc822"
        return Intent.createChooser(intent, context.getString(R.string.send_feedback_choose_email))
    }

    override val route: String
        get() = "wire-intent:report-bug"
}

object WelcomeToNewAndroidAppDestination : ExternalUriStringResDirection {
    override val uriStringRes: Int
        get() = R.string.url_welcome_to_new_android
}

object AndroidReleaseNotesDestination : ExternalUriStringResDirection {
    override val uriStringRes: Int
        get() = R.string.url_android_release_notes
}
