package com.wire.android.ui.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import com.wire.android.model.Clickable
import com.wire.android.ui.theme.WireColorScheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.id.ConversationId
import kotlin.math.absoluteValue

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
fun Tint(contentColor: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
}

@Composable
fun ImageVector.Icon(modifier: Modifier = Modifier): @Composable (() -> Unit) =
    { androidx.compose.material3.Icon(imageVector = this, contentDescription = "", modifier = modifier) }

@Composable
internal fun dimensions() = MaterialTheme.wireDimensions

@Composable
internal fun colorsScheme() = MaterialTheme.wireColorScheme

@Composable
internal fun WireColorScheme.conversationColor(id: ConversationId): Color {
    val colors = this.groupAvatarColors
    return  colors[(id.hashCode() % colors.size).absoluteValue]
}

fun LazyListState.topBarElevation(maxElevation: Dp): Dp =
    if (firstVisibleItemIndex == 0) minOf(firstVisibleItemScrollOffset.toFloat().dp, maxElevation)
    else maxElevation

fun ScrollState.topBarElevation(maxElevation: Dp): Dp = minOf(value.dp, maxElevation)

fun LazyListState.bottomBarElevation(maxElevation: Dp): Dp = layoutInfo.visibleItemsInfo.lastOrNull()?.let {
    if (it.index == layoutInfo.totalItemsCount - 1) minOf((it.offset + it.size - layoutInfo.viewportEndOffset).dp, maxElevation)
    else maxElevation
} ?: maxElevation

fun ScrollState.bottomBarElevation(maxElevation: Dp): Dp = minOf((maxValue - value).dp, maxElevation)

@Composable
fun LazyListState.rememberTopBarElevationState(maxElevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation): State<Dp> =
    remember { derivedStateOf { topBarElevation(maxElevation) } }

@Composable
fun ScrollState.rememberTopBarElevationState(maxElevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation): State<Dp> =
    remember { derivedStateOf { topBarElevation(maxElevation) } }

@Composable
fun LazyListState.rememberBottomBarElevationState(maxElevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation): State<Dp> =
    remember { derivedStateOf { bottomBarElevation(maxElevation) } }

@Composable
fun ScrollState.rememberBottomBarElevationState(maxElevation: Dp = MaterialTheme.wireDimensions.topBarShadowElevation): State<Dp> =
    remember { derivedStateOf { bottomBarElevation(maxElevation) } }

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

@Composable
fun Modifier.clickable(clickable: Clickable) = this.clickable(
    enabled = clickable.enabled,
    onClick = clickable.onClick
)
