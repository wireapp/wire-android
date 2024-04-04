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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import com.wire.android.model.Clickable
import com.wire.android.ui.home.conversations.model.messagetypes.asset.UIAssetMessage
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.message.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

// todo try to move as much as we can to common

@Composable
fun Modifier.selectableBackground(isSelected: Boolean, onClick: () -> Unit): Modifier =
    this.selectable(
        selected = isSelected,
        onClick = { onClick() },
        interactionSource = remember { MutableInteractionSource() },
        indication = rememberRipple(bounded = true, color = MaterialTheme.colorScheme.onBackground.copy(0.5f)),
        role = Role.Tab
    )

@Composable
fun Modifier.shimmerPlaceholder(
    visible: Boolean,
    color: Color = MaterialTheme.wireColorScheme.background,
    shimmerColor: Color = MaterialTheme.wireColorScheme.surface,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.placeholderShimmerCornerSize)
) = this.placeholder(
    visible = visible,
    highlight = PlaceholderHighlight.shimmer(shimmerColor),
    color = color,
    shape = shape,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.clickable(clickable: Clickable?) = clickable?.let {
    val onClick = rememberClickBlockAction(clickable.clickBlockParams, clickable.onClick)
    val onLongClick = clickable.onLongClick?.let { onLongClick ->
        rememberClickBlockAction(clickable.clickBlockParams, onLongClick)
    }
    this.combinedClickable(
        enabled = clickable.enabled,
        onClick = onClick,
        onLongClick = onLongClick
    )
} ?: this

@Composable
fun <T> rememberFlow(
    flow: Flow<T>,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
): Flow<T> {
    return remember(key1 = flow, key2 = lifecycleOwner) { flow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED) }
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
    val currentYear = Instant.fromEpochMilliseconds(System.currentTimeMillis()).toLocalDateTime(
        TimeZone.currentSystemDefault()).year
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

fun List<UIAssetMessage>.toImageAssetGroupedByMonthAndYear(timeZone: TimeZone) = this.groupBy { asset ->
    val localDateTime = asset.time.toLocalDateTime(timeZone)
    monthYearHeader(year = localDateTime.year, month = localDateTime.monthNumber)
}

fun List<Message.Standalone>.toGenericAssetGroupedByMonthAndYear(timeZone: TimeZone) = this.groupBy { message ->
    val localDateTime = message.date.toInstant().toLocalDateTime(timeZone)
    monthYearHeader(year = localDateTime.year, month = localDateTime.monthNumber)
}
