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

package com.wire.android.ui.drawer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ramcosta.composedestinations.spec.Direction
import com.wire.android.R
import com.wire.android.ui.common.Logo
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.EmailComposer.Companion.giveFeedbackEmailTemplate
import com.wire.android.util.EmailComposer.Companion.reportBugEmailTemplate
import com.wire.android.util.getDeviceId
import com.wire.android.util.getGitBuildId
import com.wire.android.util.getUrisOfFilesInDirectory
import com.wire.android.util.multipleFileSharingIntent
import com.wire.android.util.sha256
import java.io.File

@Composable
// TODO: logFilePath does not belong in the UI logic
fun HomeDrawer(
    logFilePath: String,
    currentRoute: String?,
    navigateToHomeItem: (Direction) -> Unit,
    navigateToItem: (Direction) -> Unit,
    onCloseDrawer: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(
                start = MaterialTheme.wireDimensions.homeDrawerHorizontalPadding,
                end = MaterialTheme.wireDimensions.homeDrawerHorizontalPadding,
                bottom = MaterialTheme.wireDimensions.homeDrawerBottomPadding
            )

    ) {
        Logo(
            modifier = Modifier
                .padding(
                    horizontal = MaterialTheme.wireDimensions.homeDrawerLogoHorizontalPadding,
                    vertical = MaterialTheme.wireDimensions.homeDrawerLogoVerticalPadding
                )
                .width(MaterialTheme.wireDimensions.homeDrawerLogoWidth)
                .height(MaterialTheme.wireDimensions.homeDrawerLogoHeight)
        )

        DrawerDestination.values().forEach { item ->
            Log.d("navigation", "HomeDrawer: currentRoute: $currentRoute")
            Log.d("navigation", "HomeDrawer: currentRoute: ${item.direction?.route}")
            if (item.isVisible) {
                DrawerItem(
                    label = item.label,
                    icon = item.icon,
                    selected = currentRoute == item.direction?.route,
                    onItemClick = remember {
                        {
                            item.direction?.let { navigateToItem(it) } ?: run {
                                if (item == DrawerDestination.ReportBug) {
                                    openReportBugIntent(context, logFilePath)
                                } else if (item == DrawerDestination.Feedback) {
                                    openFeedbackIntent(context)
                                }
                            }
                        }
                    }
                )
            }
            if (item.shouldAddSpacer) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun openReportBugIntent(
    context: Context,
    logFilePath: String,
) {
    File(logFilePath).parentFile?.let { file ->
        val logsUris = context.getUrisOfFilesInDirectory(file)
        multipleFileSharingIntent(logsUris).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf("wire-newandroid@wearezeta.zendesk.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Bug Report - Wire Beta")
            putExtra(Intent.EXTRA_TEXT, reportBugEmailTemplate(context.getDeviceId()?.sha256(), context.getGitBuildId()))
            type = "message/rfc822"
        }.run {
            context.startActivity(Intent.createChooser(this, context.getString(R.string.send_feedback_choose_email)))
        }
    }
}

private fun openFeedbackIntent(context: Context) {
    Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf("wire-newandroid-feedback@wearezeta.zendesk.com"))
        putExtra(Intent.EXTRA_SUBJECT, "Feedback - Wire Beta")
        putExtra(Intent.EXTRA_TEXT, giveFeedbackEmailTemplate(context.getDeviceId()?.sha256(), context.getGitBuildId()))

        selector = Intent(Intent.ACTION_SENDTO).setData(Uri.parse("mailto:"))
    }.run {
        context.startActivity(Intent.createChooser(this, context.getString(R.string.send_feedback_choose_email)))
    }
}
