/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.meetings.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.meetings.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.colorsScheme

@Composable
fun NewMeetingBottomSheet(
    sheetState: WireModalSheetState<Unit>,
    onMeetNowClick: () -> Unit,
    onScheduleClick: () -> Unit,
) {
    WireModalSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            WireMenuModalSheetContent(
                header = MenuModalSheetHeader.Visible(title = stringResource(R.string.new_meeting_title)),
                menuItems = buildList<@Composable () -> Unit> {
                    add {
                        MenuBottomSheetItem(
                            title = stringResource(R.string.new_meeting_now),
                            leading = {
                                MenuItemIcon(
                                    id = com.wire.android.ui.common.R.drawable.ic_video_call,
                                    tint = colorsScheme().onSurface,
                                    contentDescription = stringResource(R.string.content_description_new_meeting_now_icon),
                                )
                            },
                            onItemClick = onMeetNowClick,
                        )
                    }
                    add {
                        MenuBottomSheetItem(
                            title = stringResource(R.string.new_meeting_schedule),
                            leading = {
                                MenuItemIcon(
                                    id = com.wire.android.ui.common.R.drawable.ic_calendar,
                                    tint = colorsScheme().onSurface,
                                    contentDescription = stringResource(R.string.content_description_new_meeting_schedule_icon),
                                )
                            },
                            onItemClick = onScheduleClick,
                        )
                    }
                }
            )
        }
    )
}
