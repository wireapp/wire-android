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

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.common.R as commonR

/**
 * One-time tip about bulk selection, which is otherwise hidden behind a long press. A small looping demo shows the
 * gesture: a finger long presses a row, which gets selected, then drags down selecting the rows below.
 */
@Composable
internal fun BulkSelectionHint(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x)
            .background(color = colorsScheme().surfaceDim, shape = RoundedCornerShape(dimensions().corner12x))
            .padding(start = dimensions().spacing12x, top = dimensions().spacing12x, bottom = dimensions().spacing12x),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing12x),
    ) {
        SelectionGestureDemo()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
        ) {
            NewFeatureBadge()
            Text(
                text = stringResource(R.string.cells_bulk_selection_hint_title),
                style = typography().title03,
            )
            Text(
                text = stringResource(R.string.cells_bulk_selection_hint_message),
                style = typography().subline01,
                color = colorsScheme().secondaryText,
            )
        }
        Icon(
            painter = painterResource(commonR.drawable.ic_close),
            contentDescription = stringResource(R.string.cells_bulk_selection_hint_dismiss_content_description),
            modifier = Modifier
                .align(Alignment.Top)
                .padding(end = dimensions().spacing4x)
                .clip(CircleShape)
                .clickable(onClick = onDismiss)
                .padding(dimensions().spacing8x)
                .size(dimensions().spacing12x),
        )
    }
}

// Static on purpose: the demo next to it already draws the eye.
@Composable
private fun NewFeatureBadge() {
    Text(
        text = stringResource(R.string.cells_new_feature_badge),
        style = typography().badge01,
        color = colorsScheme().onPrimary,
        modifier = Modifier
            .background(color = colorsScheme().primary, shape = RoundedCornerShape(dimensions().corner12x))
            .padding(horizontal = dimensions().spacing6x, vertical = dimensions().spacing2x),
    )
}

@Composable
private fun SelectionGestureDemo() {
    // Static previews don't run effects, so they show the still frame too.
    val animationsEnabled = rememberAnimationsEnabled() && !LocalInspectionMode.current
    val progress = remember { Animatable(if (animationsEnabled) 0f else STATIC_FRAME) }
    LaunchedEffect(animationsEnabled) {
        // With animations off, animateTo would complete at once and this loop would spin: stay on a still frame instead.
        if (!animationsEnabled) return@LaunchedEffect
        while (true) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = LOOP_DURATION_MS, easing = LinearEasing))
        }
    }

    // Resolved here: dimensions() is composable, the drawing below isn't.
    val sizes = DemoSizes(
        rowGap = dimensions().spacing4x,
        rowCorner = dimensions().corner4x,
        indicatorStroke = dimensions().spacing1x,
        fingerRadius = dimensions().spacing6x,
        fingerStroke = dimensions().spacing1x,
        rippleRadius = dimensions().spacing18x,
    )
    val colors = DemoColors(
        row = colorsScheme().surface,
        selectedRow = colorsScheme().primaryVariant,
        line = colorsScheme().outline,
        indicatorBorder = colorsScheme().onPrimaryButtonDisabled,
        indicator = colorsScheme().primary,
        check = colorsScheme().onPrimary,
        finger = colorsScheme().primary,
    )
    Canvas(
        modifier = Modifier
            .size(width = dimensions().spacing80x, height = dimensions().spacing64x)
            // Purely illustrative: the text next to it carries the information.
            .clearAndSetSemantics {}
    ) {
        // Read in the draw phase only, so the animation redraws without recomposing.
        drawDemoFrame(t = progress.value, sizes = sizes, colors = colors)
    }
}

private class DemoSizes(
    val rowGap: Dp,
    val rowCorner: Dp,
    val indicatorStroke: Dp,
    val fingerRadius: Dp,
    val fingerStroke: Dp,
    val rippleRadius: Dp,
)

private class DemoColors(
    val row: Color,
    val selectedRow: Color,
    val line: Color,
    val indicatorBorder: Color,
    val indicator: Color,
    val check: Color,
    val finger: Color,
)

/** Draws the demo at [t], the position in the loop from 0 to 1. */
private fun DrawScope.drawDemoFrame(t: Float, sizes: DemoSizes, colors: DemoColors) {
    val rowGap = sizes.rowGap.toPx()
    val rowHeight = (size.height - rowGap * (ROW_COUNT - 1)) / ROW_COUNT
    fun rowCenterY(row: Int) = row * (rowHeight + rowGap) + rowHeight / 2

    val dragProgress = FastOutSlowInEasing.transform(phase(t, DRAG_START, DRAG_END))
    val fingerY = rowCenterY(0) + (rowCenterY(ROW_COUNT - 1) - rowCenterY(0)) * dragProgress
    val clearing = 1f - phase(t, CLEAR_START, CLEAR_END)

    repeat(ROW_COUNT) { row ->
        // The first row is selected by the long press, the others as the finger reaches them.
        val selectedAt = if (row == 0) PRESS_END else DRAG_START + (DRAG_END - DRAG_START) * row / (ROW_COUNT - 1)
        val selection = phase(t, selectedAt, selectedAt + SELECT_DURATION) * clearing
        drawDemoRow(top = row * (rowHeight + rowGap), height = rowHeight, selection = selection, sizes = sizes, colors = colors)
    }

    val fingerX = size.width * FINGER_X_RATIO
    val fingerAlpha = phase(t, 0f, APPEAR_END) * (1f - phase(t, LIFT_START, LIFT_END))
    // The long press: a ripple growing under the finger until the row gets selected.
    val press = phase(t, APPEAR_END, PRESS_END)
    if (press > 0f && press < 1f) {
        drawCircle(
            color = colors.finger.copy(alpha = RIPPLE_ALPHA * (1f - press)),
            radius = sizes.rippleRadius.toPx() * press,
            center = Offset(fingerX, rowCenterY(0)),
        )
    }
    if (fingerAlpha > 0f) {
        val center = Offset(fingerX, fingerY)
        drawCircle(color = colors.finger.copy(alpha = FINGER_FILL_ALPHA * fingerAlpha), radius = sizes.fingerRadius.toPx(), center = center)
        drawCircle(
            color = colors.finger.copy(alpha = fingerAlpha),
            radius = sizes.fingerRadius.toPx(),
            center = center,
            style = Stroke(width = sizes.fingerStroke.toPx()),
        )
    }
}

@Suppress("MagicNumber")
private fun DrawScope.drawDemoRow(top: Float, height: Float, selection: Float, sizes: DemoSizes, colors: DemoColors) {
    drawRoundRect(
        color = lerp(colors.row, colors.selectedRow, selection),
        topLeft = Offset(0f, top),
        size = Size(size.width, height),
        cornerRadius = CornerRadius(sizes.rowCorner.toPx()),
    )

    // Selection indicator, like the list's: an empty circle that fills up with a check mark.
    val indicatorRadius = height * INDICATOR_RADIUS_RATIO
    val indicatorCenter = Offset(height / 2, top + height / 2)
    drawCircle(
        color = colors.indicatorBorder,
        radius = indicatorRadius,
        center = indicatorCenter,
        style = Stroke(width = sizes.indicatorStroke.toPx()),
    )
    if (selection > 0f) {
        drawCircle(color = colors.indicator, radius = indicatorRadius * selection, center = indicatorCenter)
        drawCheckMark(center = indicatorCenter, radius = indicatorRadius, alpha = selection, color = colors.check)
    }

    // Name and details lines.
    val lineStart = height
    val lineHeight = height * LINE_HEIGHT_RATIO
    drawRoundRect(
        color = colors.line,
        topLeft = Offset(lineStart, top + height / 2 - lineHeight * 1.5f),
        size = Size((size.width - lineStart) * NAME_LINE_RATIO, lineHeight),
        cornerRadius = CornerRadius(lineHeight / 2),
    )
    drawRoundRect(
        color = colors.line.copy(alpha = colors.line.alpha * DETAILS_LINE_ALPHA),
        topLeft = Offset(lineStart, top + height / 2 + lineHeight * 0.5f),
        size = Size((size.width - lineStart) * DETAILS_LINE_RATIO, lineHeight),
        cornerRadius = CornerRadius(lineHeight / 2),
    )
}

@Suppress("MagicNumber")
private fun DrawScope.drawCheckMark(center: Offset, radius: Float, alpha: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x - radius * 0.45f, center.y)
        lineTo(center.x - radius * 0.1f, center.y + radius * 0.35f)
        lineTo(center.x + radius * 0.45f, center.y - radius * 0.3f)
    }
    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(width = radius * 0.3f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/** How far [t] is between [start] and [end], from 0 before it to 1 after it. */
private fun phase(t: Float, start: Float, end: Float): Float = ((t - start) / (end - start)).coerceIn(0f, 1f)

/** False when the user turned system animations off (developer options or accessibility "remove animations"). */
@Composable
private fun rememberAnimationsEnabled(): Boolean {
    val contentResolver = LocalContext.current.contentResolver
    return remember(contentResolver) {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}

private const val LOOP_DURATION_MS = 3600
private const val ROW_COUNT = 3

// Timeline, as fractions of the loop.
private const val APPEAR_END = 0.08f
private const val PRESS_END = 0.30f
private const val SELECT_DURATION = 0.06f
private const val DRAG_START = 0.38f
private const val DRAG_END = 0.66f
private const val LIFT_START = 0.72f
private const val LIFT_END = 0.80f
private const val CLEAR_START = 0.86f
private const val CLEAR_END = 0.94f

// Frame shown when animations are off: every row selected, finger still on the last one.
private const val STATIC_FRAME = 0.70f

private const val INDICATOR_RADIUS_RATIO = 0.3f
private const val LINE_HEIGHT_RATIO = 0.16f
private const val NAME_LINE_RATIO = 0.75f
private const val DETAILS_LINE_RATIO = 0.45f
private const val DETAILS_LINE_ALPHA = 0.6f
private const val FINGER_X_RATIO = 0.6f
private const val FINGER_FILL_ALPHA = 0.25f
private const val RIPPLE_ALPHA = 0.35f

@PreviewMultipleThemes
@Composable
private fun PreviewBulkSelectionHint() {
    WireTheme {
        BulkSelectionHint(onDismiss = {})
    }
}
