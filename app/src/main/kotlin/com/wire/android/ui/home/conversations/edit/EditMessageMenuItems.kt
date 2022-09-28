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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumTouchTargetEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMessageMenuItems(
    isCopyable: Boolean,
    isEditable: Boolean,
    onCopyMessage: () -> Unit,
    onDeleteMessage: () -> Unit,
    onReactionClick: (emoji: String) -> Unit,
): List<@Composable () -> Unit> {
    return buildList {
        add {
            Column() {
                Row() {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("REACTIONS", style = MaterialTheme.wireTypography.label01)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf("👍", "❤️", "🚀", "🤯", "😄", "🤣", "👎").forEach { emoji ->
                        CompositionLocalProvider(
                            LocalMinimumTouchTargetEnforcement provides false
                        ) {
                            Button(
                                onClick = {
                                    val correctedEmoji = if (emoji == "❤️") "❤"
                                    else emoji
                                    onReactionClick(correctedEmoji)
                                },
                                modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                                contentPadding = PaddingValues(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.wireColorScheme.surface,
                                    contentColor = MaterialTheme.wireColorScheme.secondaryButtonSelectedOutline
                                )
                            ) {
                                Text(emoji, style = TextStyle(fontSize = 28.sp))
                            }
                        }
                    }
                }
            }

        }
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
    }
}
