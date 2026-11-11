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
package com.wire.android.feature.cells.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.search.DriveSearchScreenType
import com.wire.android.feature.cells.ui.search.sort.SortBy
import com.wire.android.feature.cells.ui.search.sort.SortRowWithMenu
import com.wire.android.feature.cells.ui.search.sort.SortingCriteria
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.common.R as commonR

/**
 * The sort row of a file listing, with the button switching to the files saved for offline use and back when
 * [showOfflineToggle]. The offline listing has an order of its own, so while [isOfflineOnly] the row says which
 * listing is shown instead of offering to sort it.
 *
 * While [showOfflineToggleHint], a tip points at the button to explain it, until [onOfflineToggleHintDismiss].
 */
@Composable
internal fun OfflineAwareSortRow(
    sortingCriteria: SortingCriteria,
    screenType: DriveSearchScreenType,
    isOfflineOnly: Boolean,
    showOfflineToggle: Boolean,
    onSortByClicked: (SortBy) -> Unit,
    onOrderClicked: (SortingCriteria) -> Unit,
    onOfflineOnlyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showOfflineToggleHint: Boolean = false,
    onOfflineToggleHintDismiss: () -> Unit = {},
) {
    Row(
        modifier = modifier.background(colorsScheme().surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = isOfflineOnly,
            modifier = Modifier.weight(1f),
            label = "offline_aware_sort_row",
        ) { offlineOnly ->
            if (offlineOnly) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = dimensions().spacing16x,
                        vertical = dimensions().spacing8x,
                    ),
                    text = stringResource(R.string.cells_offline_files_label),
                    style = typography().button02,
                )
            } else {
                SortRowWithMenu(
                    sortingCriteria = sortingCriteria,
                    screenType = screenType,
                    onSortByClicked = onSortByClicked,
                    onOrderClicked = onOrderClicked,
                )
            }
        }
        if (showOfflineToggle) {
            OfflineToggleHintBox(
                showHint = showOfflineToggleHint,
                onHintDismiss = onOfflineToggleHintDismiss,
                modifier = Modifier.padding(end = dimensions().spacing16x),
            ) {
                WireSecondaryIconButton(
                    onButtonClicked = { onOfflineOnlyChange(!isOfflineOnly) },
                    iconResource = R.drawable.ic_wifi_off,
                    contentDescription = if (isOfflineOnly) {
                        R.string.cells_show_all_files_content_description
                    } else {
                        R.string.cells_show_offline_files_content_description
                    },
                    state = if (isOfflineOnly) WireButtonState.Selected else WireButtonState.Default,
                )
            }
        }
    }
}

/**
 * Anchors a tip explaining the offline toggle below it while [showHint]. It stays until dismissed with its button
 * or by tapping elsewhere, either calling [onHintDismiss].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineToggleHintBox(
    showHint: Boolean,
    onHintDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    LaunchedEffect(showHint) {
        if (showHint) tooltipState.show() else tooltipState.dismiss()
    }
    // The popup is placed against the bounds of the TooltipBox, its margins included: kept out of it, so that the
    // popup lines up with the button and keeps the same distance to the screen edge.
    Box(modifier = modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
            state = tooltipState,
            // Only shown on purpose: a long press on the button must not bring the tip back.
            enableUserInput = false,
            hasAction = true,
            onDismissRequest = {
                tooltipState.dismiss()
                onHintDismiss()
            },
            tooltip = {
                // Styled like the bulk selection tip, see BulkSelectionHint, with a caret pointing at the button.
                RichTooltip(
                    caretShape = TooltipDefaults.caretShape(),
                    shape = RoundedCornerShape(dimensions().corner12x),
                    colors = TooltipDefaults.richTooltipColors(
                        containerColor = colorsScheme().surfaceDim,
                        contentColor = colorsScheme().onSurface,
                        titleContentColor = colorsScheme().onSurface,
                        actionContentColor = colorsScheme().primary,
                    ),
                ) {
                    OfflineToggleHintContent(
                        onDismiss = {
                            tooltipState.dismiss()
                            onHintDismiss()
                        },
                    )
                }
            },
            // The button pulses along with the tip, to draw the eye to what the tip is about.
            content = { PulsingHalo(isPulsing = tooltipState.isVisible, content = content) },
        )
    }
}

@Composable
private fun OfflineToggleHintContent(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.padding(vertical = dimensions().spacing8x),
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing12x),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_wifi_off),
            contentDescription = null,
            tint = colorsScheme().onPrimary,
            modifier = Modifier
                .background(color = colorsScheme().primary, shape = CircleShape)
                .padding(dimensions().spacing8x)
                .size(dimensions().spacing16x),
        )
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
        ) {
            NewFeatureBadge()
            Text(
                text = stringResource(R.string.cells_offline_toggle_hint_title),
                style = typography().title03,
            )
            Text(
                text = stringResource(R.string.cells_offline_toggle_hint_message),
                style = typography().subline01,
                color = colorsScheme().secondaryText,
            )
        }
        Icon(
            painter = painterResource(commonR.drawable.ic_close),
            contentDescription = stringResource(R.string.cells_bulk_selection_hint_dismiss_content_description),
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onDismiss)
                .padding(dimensions().spacing8x)
                .size(dimensions().spacing12x),
        )
    }
}

/**
 * Draws rings growing out of [content] and fading away, over and over, while [isPulsing]. Rings are drawn around
 * [content] without taking any room, and not at all when the user turned system animations off.
 */
@Composable
private fun PulsingHalo(
    isPulsing: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    Box(modifier = modifier) {
        if (isPulsing && animationsEnabled) {
            val color = colorsScheme().primary
            val cornerRadius = MaterialTheme.wireDimensions.buttonCornerSize
            val progress by rememberInfiniteTransition(label = "offline_toggle_halo").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = HALO_PULSE_DURATION_MS, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "offline_toggle_halo_progress",
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val scale = 1f + HALO_MAX_GROWTH * progress
                        val haloSize = Size(size.width * scale, size.height * scale)
                        val radius = cornerRadius.toPx() * scale
                        drawRoundRect(
                            color = color.copy(alpha = HALO_START_ALPHA * (1f - progress)),
                            topLeft = Offset((size.width - haloSize.width) / 2, (size.height - haloSize.height) / 2),
                            size = haloSize,
                            cornerRadius = CornerRadius(radius, radius),
                        )
                    }
            )
        }
        content()
    }
}

private const val HALO_PULSE_DURATION_MS = 1400
private const val HALO_MAX_GROWTH = 0.6f
private const val HALO_START_ALPHA = 0.35f
