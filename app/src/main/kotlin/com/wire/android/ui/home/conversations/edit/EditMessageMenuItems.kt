package com.wire.android.ui.home.conversations.edit

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
import androidx.compose.material3.LocalMinimumTouchTargetEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun EditMessageMenuItems(
    isCopyable: Boolean,
    isEditable: Boolean,
    onCopyMessage: () -> Unit,
    onDeleteMessage: () -> Unit,
    onReactionClick: (emoji: String) -> Unit,
    onReply: () -> Unit,
    onMessageDetailsClick: () -> Unit
): List<@Composable () -> Unit> {
    return buildList {
        add { ReactionOptions(onReactionClick) }
        add { MessageDetails(onMessageDetailsClick) }
        add {
            if (isCopyable) {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_copy,
                            contentDescription = stringResource(R.string.content_description_copy_the_message),
                        )
                    },
                    title = stringResource(R.string.label_copy),
                    onItemClick = onCopyMessage
                )
            }
        }
        if (isEditable) {
            add {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_edit,
                            contentDescription = stringResource(R.string.content_description_edit_the_message)
                        )
                    },
                    title = stringResource(R.string.label_edit),
                )
            }
        }
        add {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_delete,
                            contentDescription = stringResource(R.string.content_description_delete_the_message),
                        )
                    },
                    title = stringResource(R.string.label_delete),
                    onItemClick = onDeleteMessage
                )
            }
        }
        add {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_speaker_on,
                            contentDescription = stringResource(R.string.content_description_delete_the_message),
                        )
                    },
                    title = "Reply",
                    onItemClick = onReply
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReactionOptions(
    onReactionClick: (emoji: String) -> Unit,
    emojiFontSize: TextUnit = 28.sp
) {
    Column {
        Row {
            Spacer(modifier = Modifier.width(dimensions().spacing8x))
            Text(
                ("${stringResource(R.string.label_reactions)} ${stringResource(id = R.string.label_more_comming_soon)}").uppercase(),
                style = MaterialTheme.wireTypography.label01
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("❤️", "👍", "😁", "🙂", "☹️", "👎").forEach { emoji ->
                CompositionLocalProvider(
                    LocalMinimumTouchTargetEnforcement provides false
                ) {
                    Button(
                        onClick = {
                            // TODO remove when all emojis will be available
                            if (emoji == "❤️") {
                                // So we display the pretty emoji,
                                // but we match the ugly one sent from other platforms
                                val correctedEmoji = "❤"
                                onReactionClick(correctedEmoji)
                            }
                        },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                            // TODO remove when all emojis will be available
                            .alpha(if (emoji == "❤️") 1F else 0.3F),
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
                    // TODO show more emojis
                },
                modifier = Modifier
                    // TODO remove when all emojis will be available
                    .alpha(0.1F),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_more_emojis),
                    contentDescription = stringResource(R.string.content_description_more_emojis)
                )
            }
        }
    }
}

@Composable
private fun MessageDetails(
    onMessageDetailsClick: () -> Unit
) {
    MenuBottomSheetItem(
        icon = {
            MenuItemIcon(
                id = R.drawable.ic_info,
                contentDescription = stringResource(R.string.content_description_open_message_details),
            )
        },
        title = stringResource(R.string.label_message_details),
        onItemClick = onMessageDetailsClick
    )
}
