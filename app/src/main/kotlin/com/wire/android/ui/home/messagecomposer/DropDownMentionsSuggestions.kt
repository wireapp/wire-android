package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.material.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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

    val defaultMaxHeightDropdownMenu = getDropDownMaxHeight(screenHeight)

    val coordinateY = updateDropDownCoordinateY(
        cursorCoordinateY,
        membersToMention.size,
        currentSelectedLineIndex,
        screenHeight
    )

    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    val density = LocalDensity.current
    val maxHeight = remember(itemHeights.toMap()) {
        return@remember calculateMaxHeight(defaultMaxHeightDropdownMenu, itemHeights, density, membersToMention)
    }

    Box(modifier = Modifier
        .fillMaxWidth()
        .offset { IntOffset(0, coordinateY) }
    ) {
        DropdownMenu(
            properties = PopupProperties(focusable = false),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight),
            expanded = true,
            onDismissRequest = {}
        ) {

            membersToMention.forEachIndexed { index, item ->
                MemberItemToMention(
                    avatarData = item.avatarData,
                    name = item.name,
                    label = item.label,
                    membership = item.membership,
                    clickable = Clickable(enabled = true) {
                        onMentionPicked(item)
                    },
                    modifier = Modifier.onSizeChanged {
                        itemHeights[index] = it.height
                    }
                )
            }
        }
    }
}

private fun calculateMaxHeight(
    defaultMaxHeightDropdownMenu: Dp,
    itemHeights: Map<Int, Int>,
    density: Density,
    membersToMention: List<Contact>,
): Dp {

    if (itemHeights.keys.toSet() != membersToMention.indices.toSet()) {
        // if we don't have all heights calculated yet, return default value
        return defaultMaxHeightDropdownMenu
    }
    val baseHeightInt = with(density) { defaultMaxHeightDropdownMenu.toPx().toInt() }

    var sum = with(density) { DropdownMenuVerticalPadding.toPx().toInt() } * TWO
    for ((i, itemSize) in itemHeights.toSortedMap()) {
        sum += itemSize
        if (sum >= baseHeightInt) {
            return with(density) { (sum - itemSize / TWO).toDp() }
        }
    }
    // all items fit into default height
    return defaultMaxHeightDropdownMenu
}

private fun getDropDownMaxHeight(screenHeight: Int): Dp {
    return if (screenHeight < MINIMUM_SCREEN_HEIGHT) DROP_DOWN_HEIGHT_SMALL else DROP_DOWN_HEIGHT_LARGE
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
    if (cursorCoordinateY >= screenHeight / HALF_SCREEN)
        coordinateY = cursorCoordinateY - THIRTY
    // If there is only one item to show, in the second part of the screen, the DropDown will be displayed above the cursor.
    // Fixing this by adding more space
    if (membersToMentionSize == ONE && cursorCoordinateY < screenHeight * EIGHTY_PERCENT)
        coordinateY = cursorCoordinateY + TWENTY
    // For the first line, we get the wrong Y coordinate of the cursor.
    // Fixing this by adding more space
    if (currentSelectedLineIndex == FIRST_LINE_INDEX)
        coordinateY = cursorCoordinateY + THIRTY
    return coordinateY.toInt()
}

private const val MINIMUM_SCREEN_HEIGHT = 700
private val DROP_DOWN_HEIGHT_SMALL = 150.dp
private val DROP_DOWN_HEIGHT_LARGE = 200.dp
private const val FIRST_LINE_INDEX = 0
private const val HALF_SCREEN = 2.5
private const val EIGHTY_PERCENT = 0.80
private const val ONE = 1
private const val TWO = 2
private const val THIRTY = 30
private const val TWENTY = 20
private val DropdownMenuVerticalPadding = 8.dp
