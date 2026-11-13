/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.meetings.ui.list

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.card.WireOutlinedCard
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.common.R as commonR

@Composable
internal fun MeetingRemindersCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val alarmManager = remember(context) { context.getSystemService(AlarmManager::class.java) }
    val isPreview = LocalInspectionMode.current
    var showMeetingRemindersCard by remember(alarmManager) {
        mutableStateOf(
            when {
                isPreview -> true
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> !alarmManager.canScheduleExactAlarms()
                else -> false
            }
        )
    }
    if (!isPreview) {
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            showMeetingRemindersCard = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()
        }
    }
    AnimatedVisibility(visible = showMeetingRemindersCard) {
        WireOutlinedCard(
            title = stringResource(R.string.meeting_reminders_card_title),
            textContent = stringResource(R.string.meeting_reminders_card_description),
            mainActionButtonText = stringResource(R.string.meeting_reminders_card_settings),
            onMainActionClick = {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                )
            },
            trailingIcon = {
                Icon(
                    painter = painterResource(commonR.drawable.ic_info),
                    contentDescription = null,
                    tint = colorsScheme().onBackground,
                )
            },
            modifier = modifier,
        )
    }
}

@PreviewMultipleThemes
@Composable
fun MeetingRemindersCardPreview() = WireTheme {
    MeetingRemindersCard()
}
