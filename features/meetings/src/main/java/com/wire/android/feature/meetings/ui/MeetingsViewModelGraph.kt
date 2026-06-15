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
@file:Suppress("MatchingDeclarationName")

package com.wire.android.feature.meetings.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.wire.android.di.PreviewProvider
import com.wire.android.di.metro.sessionKeyedAssistedMetroViewModelAs
import com.wire.android.di.metro.sessionKeyedMetroViewModelAs
import com.wire.android.feature.meetings.ui.create.NewMeetingType
import com.wire.android.feature.meetings.ui.create.NewMeetingViewModel
import com.wire.android.feature.meetings.ui.create.NewMeetingViewModelImpl
import com.wire.android.feature.meetings.ui.create.NewMeetingViewModelPreview
import com.wire.android.feature.meetings.ui.list.MeetingListViewModel
import com.wire.android.feature.meetings.ui.list.MeetingListViewModelImpl
import com.wire.android.feature.meetings.ui.options.MeetingOptionsMenuViewModel
import com.wire.android.feature.meetings.ui.options.MeetingOptionsMenuViewModelImpl
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory

interface MeetingsManualViewModelFactory : ManualViewModelAssistedFactory {
    fun meetingListViewModel(type: MeetingsTabItem): MeetingListViewModelImpl
}

@Composable
fun meetingListViewModel(
    type: MeetingsTabItem,
): MeetingListViewModel =
    sessionKeyedAssistedMetroViewModelAs<MeetingListViewModelImpl, MeetingListViewModel, MeetingsManualViewModelFactory>(
        key = "meeting_list_${type.name}",
    ) {
        meetingListViewModel(type)
    }

@Composable
fun meetingOptionsMenuListViewModel(): MeetingOptionsMenuViewModel =
    sessionKeyedMetroViewModelAs<MeetingOptionsMenuViewModelImpl, MeetingOptionsMenuViewModel>()

@Composable
fun newMeetingViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
): NewMeetingViewModel =
    sessionKeyedMetroViewModelAs<NewMeetingViewModelImpl, NewMeetingViewModel>(
        viewModelStoreOwner = viewModelStoreOwner,
        previewProvider = PreviewProvider.of(NewMeetingViewModelPreview(NewMeetingType.Schedule)),
    )
