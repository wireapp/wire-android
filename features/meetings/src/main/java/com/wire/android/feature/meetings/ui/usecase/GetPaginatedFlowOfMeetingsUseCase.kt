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
package com.wire.android.feature.meetings.ui.usecase

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.wire.android.feature.meetings.ui.MeetingsTabItem
import com.wire.kalium.logic.data.meeting.MeetingOccurrence
import com.wire.kalium.logic.feature.meeting.GetPaginatedMeetingOccurrencesUseCase
import com.wire.kalium.util.DateTimeUtil.asStartOfDay
import com.wire.kalium.util.DateTimeUtil.currentInstant
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

/**
 * This use case observes and returns a flow of paginated meeting occurrences for the UI layer.
 * TODO: MeetingsTabItem.PAST is not yet implemented, so it will return the same data as MeetingsTabItem.NEXT for now.
 */
class GetPaginatedFlowOfMeetingsUseCase @Inject constructor(
    private val getPaginatedMeetingOccurrencesUseCase: GetPaginatedMeetingOccurrencesUseCase,
) {
    suspend operator fun invoke(type: MeetingsTabItem): Flow<PagingData<MeetingOccurrence>> =
        getPaginatedMeetingOccurrencesUseCase(
            pagingConfig = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                initialLoadSize = INITIAL_LOAD_SIZE
            ),
            startingOffset = 0L,
            from = currentInstant().asStartOfDay(),
        )

    private companion object {
        const val PAGE_SIZE = 20
        const val INITIAL_LOAD_SIZE = 40
        const val PREFETCH_DISTANCE = 30
    }
}
