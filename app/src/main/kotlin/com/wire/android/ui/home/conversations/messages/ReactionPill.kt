package com.wire.android.ui.home.conversations.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun ReactionPill(
    emoji: String,
    count: Int,
    isOwn: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {

    val strokeColor = if (isOwn) {
        MaterialTheme.wireColorScheme.secondaryButtonSelectedOutline
    } else {
        MaterialTheme.wireColorScheme.primaryButtonDisabled
    }

    val backgroundColor = if (isOwn) {
        MaterialTheme.wireColorScheme.secondaryButtonSelected
    } else {
        MaterialTheme.wireColorScheme.surface
    }

    val textColor = if (isOwn) {
        MaterialTheme.wireColorScheme.primary
    } else {
        MaterialTheme.wireColorScheme.labelText
    }

    CompositionLocalProvider(
        LocalMinimumTouchTargetEnforcement provides false
    ) {
        OutlinedButton(
            onClick = onTap,
            border = BorderStroke(borderStrokeWidth, strokeColor),
            shape = RoundedCornerShape(borderRadius),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = backgroundColor),
            contentPadding = PaddingValues(horizontal = dimensions().spacing8x, vertical = dimensions().spacing4x),
            modifier = modifier.defaultMinSize(minWidth = minDimension, minHeight = minDimension)
        ) {
            Text(
                emoji,
                style = TextStyle(fontSize = reactionFontSize)
            )
            Spacer(modifier = Modifier.width(dimensions().spacing4x))
            Text(
                count.toString(),
                style = MaterialTheme.wireTypography.label02,
                color = textColor
            )
        }
    }
}

private val minDimension = 1.dp
private val borderRadius = 12.dp
private val borderStrokeWidth = 1.dp
private val reactionFontSize = 12.sp
