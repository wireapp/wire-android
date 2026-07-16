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
package com.wire.android.feature.meetings.ui.options

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.ui.list.MeetingLeadingIcon
import com.wire.android.feature.meetings.ui.meetingOptionsMenuListViewModel
import com.wire.android.feature.meetings.ui.mock.scheduledRepeatingGroupMeeting
import com.wire.android.feature.meetings.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.CurrentTimeProvider
import com.wire.android.ui.common.R as UICommonR

@Composable
@SuppressLint("ComposeModifierMissing")
fun MeetingOptionsModalSheetLayout(
    sheetState: WireModalSheetState<String>,
    viewModel: MeetingOptionsMenuViewModel = when {
        LocalInspectionMode.current -> MeetingOptionsMenuViewModelPreview(CurrentTimeProvider.Preview)
        else -> meetingOptionsMenuListViewModel()
    }
) {
    val deletedMeetingOptionsClosedMessage = stringResource(R.string.deleted_meeting_options_closed)
    val snackbarHostState = LocalSnackbarHostState.current
    WireModalSheetLayout(
        sheetState = sheetState,
        sheetContent = { occurrenceId ->
            when (val state = viewModel.observeMeetingStateFlow(occurrenceId).collectAsStateWithLifecycle().value) {
                is MeetingOptionsMenuState.Meeting -> MeetingOptionsModalContent(meetingState = state).also {
                    sheetState.updateContent()
                }

                MeetingOptionsMenuState.Loading -> WireCircularProgressIndicator( // loading state - show a progress indicator
                    progressColor = colorsScheme().onSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                MeetingOptionsMenuState.NotAvailable -> sheetState.hide { // meeting not found - hide the sheet and show info
                    snackbarHostState.showSnackbar(deletedMeetingOptionsClosedMessage)
                }
            }
        }
    )
}

@Composable
private fun MeetingOptionsModalContent(
    meetingState: MeetingOptionsMenuState.Meeting,
    onCreateConversation: () -> Unit = {},
    onCopyLink: () -> Unit = {},
    onEditMeeting: () -> Unit = {},
    onDeleteMeetingForMe: () -> Unit = {},
    onDeleteMeetingForEveryone: () -> Unit = {},
) {
    WireMenuModalSheetContent(
        header = MenuModalSheetHeader.Visible(
            title = meetingState.title,
            leadingIcon = {
                MeetingLeadingIcon(
                    padding = PaddingValues(
                        top = dimensions().spacing12x,
                        bottom = dimensions().spacing12x,
                        start = dimensions().spacing16x,
                        end = dimensions().spacing4x
                    )
                )
            },
            includeDivider = true,
            customVerticalPadding = dimensions().spacing0x
        ),
        menuItems = buildList<@Composable () -> Unit> {
            addIf(meetingState.createConversationEnabled) {
                MenuBottomSheetItem(
                    title = stringResource(R.string.meeting_options_create_conversation),
                    leading = {
                        Icon(
                            painter = painterResource(UICommonR.drawable.ic_circle_plus),
                            contentDescription = stringResource(R.string.content_description_create_conversation),
                            tint = colorsScheme().onSurface,
                        )
                    },
                    onItemClick = onCreateConversation,
                )
            }
            addIf(meetingState.copyLinkEnabled) {
                MenuBottomSheetItem(
                    title = stringResource(R.string.meeting_options_copy_link),
                    leading = {
                        Icon(
                            painter = painterResource(UICommonR.drawable.ic_link_indicator),
                            contentDescription = stringResource(R.string.content_description_copy_link),
                            tint = colorsScheme().onSurface,
                        )
                    },
                    onItemClick = onCopyLink,
                )
            }
            addIf(meetingState.editMeetingEnabled) {
                MenuBottomSheetItem(
                    title = stringResource(R.string.meeting_options_edit_meeting),
                    leading = {
                        Icon(
                            painter = painterResource(UICommonR.drawable.ic_edit),
                            contentDescription = stringResource(R.string.content_description_edit_meeting),
                            tint = colorsScheme().onSurface,
                        )
                    },
                    onItemClick = onEditMeeting,
                )
            }
            addIf(meetingState.deleteOption == MeetingOptionsMenuState.Meeting.DeleteOption.ForMe) {
                MenuBottomSheetItem(
                    title = stringResource(R.string.meeting_options_delete_meeting_for_me),
                    leading = {
                        Icon(
                            painter = painterResource(UICommonR.drawable.ic_close),
                            contentDescription = stringResource(R.string.content_description_delete_meeting_for_me),
                            tint = colorsScheme().error,
                        )
                    },
                    itemProvidedColor = colorsScheme().error,
                    onItemClick = onDeleteMeetingForMe,
                )
            }
            addIf(meetingState.deleteOption == MeetingOptionsMenuState.Meeting.DeleteOption.ForEveryone) {
                MenuBottomSheetItem(
                    title = stringResource(R.string.meeting_options_delete_meeting_for_everyone),
                    leading = {
                        Icon(
                            painter = painterResource(UICommonR.drawable.ic_delete),
                            contentDescription = stringResource(R.string.content_description_delete_meeting_for_everyone),
                            tint = colorsScheme().error,
                        )
                    },
                    itemProvidedColor = colorsScheme().error,
                    onItemClick = onDeleteMeetingForEveryone,
                )
            }
        }
    )
}

private fun <E> MutableList<E>.addIf(condition: Boolean, element: E) {
    if (condition) add(element)
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageOptionsModalSheetLayout() = WireTheme {
    MeetingOptionsModalSheetLayout(
        sheetState = rememberWireModalSheetState(
            initialValue = WireSheetValue.Expanded(CurrentTimeProvider.Preview.scheduledRepeatingGroupMeeting.occurrenceId)
        )
    )
}
