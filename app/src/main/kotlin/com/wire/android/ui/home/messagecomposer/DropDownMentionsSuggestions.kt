/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
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
    searchQuery: String,
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
                .background(MaterialTheme.colorScheme.surface)
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
                    searchQuery = searchQuery,
                    modifier = Modifier.onSizeChanged {
                        itemHeights[index] = it.height
                    }
                )
            }
        }
    }
}

@Suppress("ReturnCount", "NestedBlockDepth")
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
    for ((_, itemSize) in itemHeights.toSortedMap()) {
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
    var coordinateY = cursorCoordinateY + SIXTY
    // since we are using BottomYCoordinate of the cursor, we reduce some space from Y coordinate
    // when reaching approximately the second part of the screen with more 3 participants to show
    if (membersToMentionSize >= THREE && cursorCoordinateY >= (screenHeight * HALF_SCREEN)) {
        coordinateY = cursorCoordinateY
    }
    // when reaching approximately sixty percent of the screen with 2 participants to show
    if (membersToMentionSize == TWO && cursorCoordinateY >= (screenHeight * SIXTY_PERCENT)) {
        coordinateY = cursorCoordinateY
    }
    // when reaching approximately seventy percent of the screen with 1 participants to show
    if (membersToMentionSize == ONE && cursorCoordinateY >= (screenHeight * SEVENTY_PERCENT)) {
        coordinateY = cursorCoordinateY
    }
    // For the first line, we get a wrong Y coordinate of the cursor.
    // Fixing this by adding more space
    if (currentSelectedLineIndex == FIRST_LINE_INDEX) {
        coordinateY += TWENTY
    }
    return coordinateY.toInt()
}

private const val MINIMUM_SCREEN_HEIGHT = 700
private val DROP_DOWN_HEIGHT_SMALL = 150.dp
private val DROP_DOWN_HEIGHT_LARGE = 200.dp
private const val FIRST_LINE_INDEX = 0
private const val HALF_SCREEN = 0.45
private const val SIXTY_PERCENT = 0.65
private const val SEVENTY_PERCENT = 0.75
private const val ONE = 1
private const val TWO = 2
private const val THREE = 3
private const val SIXTY = 60
private const val TWENTY = 20
private val DropdownMenuVerticalPadding = 8.dp
