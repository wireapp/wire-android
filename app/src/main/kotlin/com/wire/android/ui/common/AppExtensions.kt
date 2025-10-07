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

package com.wire.android.ui.common

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import com.wire.android.R
import com.wire.android.model.Clickable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

// todo try to move as much as we can to common

@SuppressLint("ComposeComposableModifier")
@Composable
fun Modifier.selectableBackground(
    isSelected: Boolean,
    onClickDescription: String = stringResource(id = R.string.content_description_select_label),
    onClick: () -> Unit
): Modifier {
    val onItemClick = Clickable(
        enabled = !isSelected,
        onClick = onClick,
        onClickDescription = onClickDescription
    )

    return this
        .clickable(onItemClick)
        .semantics {
            if (isSelected) selected = true // So TalkBack ignores selection when it's not selected
        }
}

@Composable
fun <T> rememberFlow(
    flow: Flow<T>,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
): Flow<T> {
    return remember(key1 = flow, key2 = lifecycleOwner) {
        flow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
    }
}

// TODO replace by collectAsStateWithLifecycle() after updating lifecycle version to 2.6.0-alpha01 or newer
@Composable
fun <T : R, R> Flow<T>.collectAsStateLifecycleAware(
    initial: R,
    context: CoroutineContext = EmptyCoroutineContext
): State<R> {
    val lifecycleAwareFlow = rememberFlow(flow = this)
    return lifecycleAwareFlow.collectAsState(initial = initial, context = context)
}

// TODO replace by collectAsStateWithLifecycle() after updating lifecycle version to 2.6.0-alpha01 or newer
@Suppress("StateFlowValueCalledInComposition")
@Composable
fun <T> StateFlow<T>.collectAsStateLifecycleAware(
    context: CoroutineContext = EmptyCoroutineContext
): State<T> = collectAsStateLifecycleAware(value, context)

fun monthYearHeader(month: Int, year: Int): String {
    val currentYear = Instant.fromEpochMilliseconds(System.currentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).year
    val monthYearInstant = LocalDateTime(year = year, monthNumber = month, 1, 0, 0, 0)

    val monthName = monthYearInstant.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
    return if (year == currentYear) {
        // If it's the current year, display only the month name
        monthName
    } else {
        // If it's not the current year, display both the month name and the year
        "$monthName $year"
    }
}
