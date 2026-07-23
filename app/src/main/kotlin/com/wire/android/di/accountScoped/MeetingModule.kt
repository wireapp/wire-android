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
package com.wire.android.di.accountScoped

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.meeting.GetPaginatedMeetingOccurrencesUseCase
import com.wire.kalium.logic.feature.meeting.MeetingScope
import com.wire.kalium.logic.feature.meeting.ObserveMeetingOccurrenceUseCase
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
class MeetingModule {

    @Provides
    fun provideMeetingScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount accountId: UserId,
    ): MeetingScope = coreLogic.getSessionScope(accountId).meetings

    @Provides
    fun provideGetPaginatedMeetingOccurrencesUseCase(meetingScope: MeetingScope): GetPaginatedMeetingOccurrencesUseCase =
        meetingScope.getPaginatedMeetingOccurrenceDetails

    @Provides
    fun provideObserveMeetingOccurrenceDetailsUseCase(meetingScope: MeetingScope): ObserveMeetingOccurrenceUseCase =
        meetingScope.observeMeetingOccurrence

    @Provides
    fun provideDeleteMeetingUseCase(meetingScope: MeetingScope): com.wire.kalium.logic.feature.meeting.DeleteMeetingUseCase =
        meetingScope.deleteMeeting
}
