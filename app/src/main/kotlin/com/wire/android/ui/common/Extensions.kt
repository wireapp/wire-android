package com.wire.android.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import com.wire.android.model.Clickable
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions

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
    this.combinedClickable(
        enabled = clickable.enabled,
        onClick = clickable.onClick,
        onLongClick = clickable.onLongClick
    )
} ?: this
