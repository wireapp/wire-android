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
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.model.MeetingItem
import com.wire.android.feature.meetings.ui.list.MeetingLeadingIcon
import com.wire.android.feature.meetings.ui.list.VideoCallIcon
import com.wire.android.feature.meetings.ui.mock.scheduledRepeatingGroupMeeting
import com.wire.android.feature.meetings.ui.util.CurrentTimeProvider
import com.wire.android.feature.meetings.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.common.R as UICommonR

@Composable
@SuppressLint("ComposeModifierMissing")
fun MeetingOptionsModalSheetLayout(
    sheetState: WireModalSheetState<String>,
    viewModel: MeetingOptionsMenuViewModel = when {
        LocalInspectionMode.current -> MeetingOptionsMenuViewModelPreview(CurrentTimeProvider.Preview)
        else -> hiltViewModel<MeetingOptionsMenuViewModelImpl>()
    }
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    WireModalSheetLayout(
        sheetState = sheetState,
        sheetContent = { meetingId ->
            when (val state = viewModel.observeMeetingStateFlow(meetingId).collectAsStateWithLifecycle().value) {
                is MeetingOptionsMenuState.Meeting -> MeetingOptionsModalContent(meeting = state.meeting).also {
                    sheetState.updateContent()
                }

                MeetingOptionsMenuState.Loading -> WireCircularProgressIndicator( // loading state - show a progress indicator
                    progressColor = colorsScheme().onSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                MeetingOptionsMenuState.NotAvailable -> sheetState.hide { // meeting not found - hide the sheet and show info
                    snackbarHostState.showSnackbar(context.getString(R.string.deleted_meeting_options_closed))
                }
            }
        }
    )
}

@Composable
private fun MeetingOptionsModalContent(
    meeting: MeetingItem,
    onStartMeeting: () -> Unit = {},
    onCreateConversation: () -> Unit = {},
    onCopyLink: () -> Unit = {},
    onEditMeeting: () -> Unit = {},
    onDeleteMeetingForMe: () -> Unit = {},
    onDeleteMeetingForEveryone: () -> Unit = {},
) {
    WireMenuModalSheetContent(
        header = MenuModalSheetHeader.Visible(
            title = meeting.title,
            leadingIcon = { MeetingLeadingIcon() },
            includeDivider = true
        ),
        menuItems = buildList<@Composable () -> Unit> {
            add {
                MenuBottomSheetItem(
                    title = stringResource(R.string.meeting_options_start_meeting),
                    leading = { VideoCallIcon(tint = colorsScheme().onSurface) },
                    onItemClick = onStartMeeting,
                )
            }
            addIf(meeting.selfRole == MeetingItem.SelfRole.Admin) {
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
            addIf(meeting.selfRole == MeetingItem.SelfRole.Admin) {
                MenuBottomSheetItem(
                    title = stringResource(R.string.meeting_options_copy_link),
                    leading = {
                        Icon(
                            painter = painterResource(UICommonR.drawable.ic_link),
                            contentDescription = stringResource(R.string.content_description_copy_link),
                            tint = colorsScheme().onSurface,
                        )
                    },
                    onItemClick = onCopyLink,
                )
            }
            addIf(meeting.selfRole == MeetingItem.SelfRole.Admin) {
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
            add {
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
            addIf(meeting.selfRole == MeetingItem.SelfRole.Admin) {
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
            initialValue = WireSheetValue.Expanded(CurrentTimeProvider.Preview.scheduledRepeatingGroupMeeting.meetingId)
        )
    )
}
