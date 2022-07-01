package com.wire.android.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.theme.WireColorScheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.kalium.logic.data.id.ConversationId
import kotlin.math.absoluteValue

@Composable
internal fun dimensions() = MaterialTheme.wireDimensions

@Composable
internal fun colorsScheme() = MaterialTheme.wireColorScheme

@Composable
internal fun WireColorScheme.conversationColor(id: ConversationId): Color {
    val colors = this.groupAvatarColors
    return  colors[(id.hashCode() % colors.size).absoluteValue]
}
