package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.material.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.wire.android.model.Clickable
import com.wire.android.ui.home.conversations.mention.MemberItemToMention
import com.wire.android.ui.home.newconversation.model.Contact

@Composable
fun DropDownMentionsSuggestions(
    currentSelectedLineIndex: Int,
    cursorCoordinateY: Float,
    membersToMention: List<Contact>,
    onMentionPicked: (Contact) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp

    val maxHeightDropdownMenu = getDropDownMaxHeight(screenHeight)

    val coordinateY = updateDropDownCoordinateY(
        cursorCoordinateY,
        membersToMention.size,
        currentSelectedLineIndex,
        screenHeight
    )

    Box(modifier = Modifier
        .fillMaxWidth()
        .offset { IntOffset(0, coordinateY) }
    ) {
        DropdownMenu(
            properties = PopupProperties(focusable = false),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeightDropdownMenu),
            expanded = true,
            onDismissRequest = {}
        ) {
            membersToMention.forEach {
                MemberItemToMention(
                    avatarData = it.avatarData,
                    name = it.name,
                    label = it.label,
                    membership = it.membership,
                    clickable = Clickable(enabled = true) {
                        onMentionPicked(it)
                    }
                )
            }
        }
    }
}

private fun getDropDownMaxHeight(screenHeight: Int): Dp {
    return if (screenHeight < 700) 150.dp else 200.dp
}

/**
 * This is mainly added to fix some wrong positions of the dropdown.
 * we get this wrong position when the DropDown moves up and down of the cursor based on the available space.
 * And since we are using BottomYCoordinate of the cursor, we need to fix that manually.
 *
 */
private fun updateDropDownCoordinateY(
    cursorCoordinateY: Float,
    membersToMentionSize: Int,
    currentSelectedLineIndex: Int,
    screenHeight: Int
): Int {
    var coordinateY = cursorCoordinateY
    // since we are using BottomYCoordinate of the cursor, we reduce some space from Y coordinate
    // for approximately the second part of the screen.
    if (cursorCoordinateY >= screenHeight / 2.5)
        coordinateY = cursorCoordinateY - 30
    // If there is only one item to show, in the second part of the screen, the DropDown will be displayed above the cursor.
    // Fixing this by adding more space
    if (membersToMentionSize == 1 && cursorCoordinateY < screenHeight * 0.80)
        coordinateY = cursorCoordinateY + 20
    // For the first line, we get the wrong Y coordinate of the cursor.
    // Fixing this by adding more space
    if (currentSelectedLineIndex == 0)
        coordinateY = cursorCoordinateY + 30
    return coordinateY.toInt()
}
