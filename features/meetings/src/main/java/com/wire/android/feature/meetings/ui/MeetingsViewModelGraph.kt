/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.metro.metroViewModel
import com.wire.android.feature.meetings.ui.create.NewMeetingType
import com.wire.android.feature.meetings.ui.create.NewMeetingViewModel
import com.wire.android.feature.meetings.ui.create.NewMeetingViewModelImpl
import com.wire.android.feature.meetings.ui.list.MeetingListViewModel
import com.wire.android.feature.meetings.ui.list.MeetingListViewModelImpl
import com.wire.android.feature.meetings.ui.options.MeetingOptionsMenuViewModel
import com.wire.android.feature.meetings.ui.options.MeetingOptionsMenuViewModelImpl

interface MeetingsViewModelGraph : MetroViewModelGraph {
    val meetingsViewModelFactory: MeetingsViewModelFactory
}

@Composable
fun meetingListViewModel(type: MeetingsTabItem): MeetingListViewModel =
    metroViewModel<MeetingsViewModelGraph, MeetingListViewModelImpl>(key = "meeting_list_${type.name}") {
        meetingsViewModelFactory.meetingListViewModel(type)
    }

@Composable
fun meetingOptionsMenuListViewModel(): MeetingOptionsMenuViewModel =
    metroViewModel<MeetingsViewModelGraph, MeetingOptionsMenuViewModelImpl> {
        meetingsViewModelFactory.meetingOptionsMenuViewModel()
    }

@Composable
fun newMeetingViewModel(type: NewMeetingType): NewMeetingViewModel =
    metroViewModel<MeetingsViewModelGraph, NewMeetingViewModelImpl>(key = "new_meeting_${type.name}") {
        meetingsViewModelFactory.newMeetingViewModel(type)
    }
