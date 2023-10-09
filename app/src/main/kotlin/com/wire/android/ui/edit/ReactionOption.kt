package com.wire.android.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.emoji.EmojiPickerBottomSheet
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionOption(
    onReactionClick: (emoji: String) -> Unit,
    emojiFontSize: TextUnit = 28.sp
) {
    var isEmojiPickerVisible by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.secondary) {
        Column {
            Row {
                Spacer(modifier = Modifier.width(dimensions().spacing8x))
                Text(
                    stringResource(R.string.label_reactions).uppercase(),
                    style = MaterialTheme.wireTypography.label01
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("❤️", "👍", "😁", "🙂", "☹️", "👎").forEach { emoji ->
                    CompositionLocalProvider(
                        LocalMinimumInteractiveComponentEnforcement provides false
                    ) {
                        Button(
                            onClick = {
                                onReactionClick(emoji)
                            },
                            modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                            contentPadding = PaddingValues(dimensions().spacing8x),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.wireColorScheme.surface,
                                contentColor = MaterialTheme.wireColorScheme.secondaryButtonSelectedOutline
                            )
                        ) {
                            Text(emoji, style = TextStyle(fontSize = emojiFontSize))
                        }
                    }
                }
                IconButton(
                    onClick = {
                        isEmojiPickerVisible = true
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_emojis),
                        contentDescription = stringResource(R.string.content_description_more_emojis)
                    )
                }
            }
        }
    }
    EmojiPickerBottomSheet(
        isVisible = isEmojiPickerVisible,
        onDismiss = {
            isEmojiPickerVisible = false
        },
        onEmojiSelected = {
            onReactionClick(it)
            isEmojiPickerVisible = false
        }
    )
}

@PreviewMultipleThemes
@Composable
private fun BasePreview() = WireTheme(isPreview = true) {
    ReactionOption(onReactionClick = {})
}
