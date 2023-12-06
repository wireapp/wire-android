/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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

package com.wire.android.ui.home.conversations.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.wire.android.model.Clickable
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.MediaAssetImage
import com.wire.android.ui.home.conversations.model.messagetypes.asset.UIAssetMessage
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.theme.wireColorScheme
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.util.map.forEachIndexed
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AssetGrid(
    uiAssetMessageList: List<UIAssetMessage>,
    modifier: Modifier = Modifier,
    onImageFullScreenMode: (conversationId: ConversationId, messageId: String, isSelfAsset: Boolean) -> Unit,
    continueAssetLoading: (shouldContinue: Boolean) -> Unit
) {
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val groupedAssets = remember(uiAssetMessageList) { groupAssetsByMonthYear(uiAssetMessageList, timeZone) }

    val scrollState = rememberLazyGridState()
    val shouldContinue by remember {
        derivedStateOf {
            !scrollState.canScrollForward
        }
    }

    // act when end of list reached
    LaunchedEffect(shouldContinue) {
        continueAssetLoading(shouldContinue)
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(color = colorsScheme().backgroundVariant)
    ) {
        val screenWidth = maxWidth
        val horizontalPadding = dimensions().spacing12x
        val itemSpacing = dimensions().spacing2x * 2
        val totalItemSpacing = itemSpacing * COLUMN_COUNT
        val availableWidth = screenWidth - horizontalPadding - totalItemSpacing
        val itemSize = availableWidth / COLUMN_COUNT

        LazyVerticalGrid(
            columns = GridCells.Fixed(COLUMN_COUNT),
            state = scrollState,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            groupedAssets.forEachIndexed { index, entry ->
                val label = entry.key
                item(
                    key = entry.key,
                    span = { GridItemSpan(COLUMN_COUNT) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                bottom = dimensions().spacing6x,
                                // first label should not have top padding
                                top = if (index == 0) dimensions().spacing0x else dimensions().spacing6x,
                            )
                    ) {
                        FolderHeader(
                            name = label.uppercase(),
                            modifier = Modifier
                                .background(MaterialTheme.wireColorScheme.background)
                                .fillMaxWidth()
                        )
                    }
                }

                items(
                    count = entry.value.size,
                    key = { entry.value[it].assetId }
                ) {
                    val uiAsset = entry.value[it]
                    val currentOnImageClick = remember(uiAsset) {
                        Clickable(enabled = true, onClick = {
                            onImageFullScreenMode(
                                uiAsset.conversationId, uiAsset.messageId, uiAsset.isSelfAsset
                            )
                        })
                    }
                    Box(
                        modifier = Modifier
                            .padding(all = dimensions().spacing2x)
                    ) {
                        MediaAssetImage(
                            asset = null,
                            width = itemSize,
                            height = itemSize,
                            downloadStatus = uiAsset.downloadStatus,
                            onImageClick = currentOnImageClick,
                            assetPath = uiAsset.assetPath
                        )
                    }
                }
            }
        }
    }
}

fun monthYearHeader(month: Int, year: Int): String {
    val currentYear = Instant.fromEpochMilliseconds(System.currentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).year
    val monthYearInstant = LocalDateTime(year = year, monthNumber = month, 1, 0, 0, 0)

    val monthName = monthYearInstant.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
    return if (year == currentYear) {
        // If it's the current year, display only the month name
        monthName
    } else {
        // If it's not the current year, display both the month name and the year
        "$monthName $year"
    }
}

fun groupAssetsByMonthYear(uiAssetMessageList: List<UIAssetMessage>, timeZone: TimeZone): Map<String, List<UIAssetMessage>> {
    return uiAssetMessageList.groupBy { asset ->
        val localDateTime = asset.time.toLocalDateTime(timeZone)
        monthYearHeader(year = localDateTime.year, month = localDateTime.monthNumber)
    }
}

private const val COLUMN_COUNT = 4
