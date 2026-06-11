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

import androidx.lifecycle.ViewModel
import com.wire.android.feature.meetings.ui.create.NewMeetingType
import com.wire.android.feature.meetings.ui.create.NewMeetingViewModelImpl
import com.wire.android.feature.meetings.ui.list.MeetingListViewModelImpl
import com.wire.android.feature.meetings.ui.options.MeetingOptionsMenuViewModelImpl
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
object MeetingsMetroViewModelBindings {

    @Provides
    @IntoMap
    @ManualViewModelAssistedFactoryKey(MeetingsManualViewModelFactory::class)
    fun meetingsManualViewModelFactory(factory: MeetingsViewModelFactory): ManualViewModelAssistedFactory =
        object : MeetingsManualViewModelFactory {
            override fun meetingListViewModel(type: MeetingsTabItem): MeetingListViewModelImpl =
                factory.meetingListViewModel(type)

            override fun newMeetingViewModel(type: NewMeetingType): NewMeetingViewModelImpl =
                factory.newMeetingViewModel(type)
        }

    @Provides
    @IntoMap
    @ViewModelKey(MeetingOptionsMenuViewModelImpl::class)
    fun meetingOptionsMenuViewModel(factory: MeetingsViewModelFactory): ViewModel =
        factory.meetingOptionsMenuViewModel()
}
