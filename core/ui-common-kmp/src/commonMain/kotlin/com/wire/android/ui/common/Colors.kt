package com.wire.android.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDarkColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireFixedColorScheme
import com.wire.android.ui.theme.wireLightColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.theme.withAccent
import kotlin.math.absoluteValue

@Composable
fun dimensions() = MaterialTheme.wireDimensions

@Composable
fun colorsScheme(accent: Accent? = null) = accent?.let {
    MaterialTheme.wireColorScheme.withAccent(accent)
} ?: MaterialTheme.wireColorScheme

@Composable
fun darkColorsScheme() = MaterialTheme.wireDarkColorScheme

@Composable
fun lightColorsScheme() = MaterialTheme.wireLightColorScheme

@Composable
fun fixedColorsScheme() = MaterialTheme.wireFixedColorScheme

@Composable
fun typography() = MaterialTheme.wireTypography

@Stable
internal fun <T> List<T>.withConversationIdValue(idValue: String): T {
    val hash = idValue.lowercase().hashCode()
    return this[hash.absoluteValue % this.size]
}
