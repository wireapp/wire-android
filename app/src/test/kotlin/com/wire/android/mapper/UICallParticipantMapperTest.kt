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
 *
 *
 */

package com.wire.android.mapper

import com.wire.kalium.logic.data.call.Participant
import com.wire.kalium.logic.data.id.QualifiedID
import io.mockk.MockKAnnotations
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class UICallParticipantMapperTest {

    @Test
    fun givenParticipant_whenMappingToUICallParticipant_thenCorrectValuesShouldBeReturned() = runTest {
        val (_, mapper) = Arrangement().arrange()
        // Given
        val item = Participant(
            id = QualifiedID("value", "domain"),
            clientId = "clientId",
            name = "name",
            isMuted = false,
            isCameraOn = false,
            isSpeaking = false,
            isSharingScreen = false,
            hasEstablishedAudio = true,
        )
        // When
        val result = mapper.toUICallParticipant(item)
        // Then
        assert(
            result.id == item.id && result.clientId == item.clientId && result.name == item.name && result.isMuted == item.isMuted
                    && result.isSpeaking == item.isSpeaking && result.avatar?.userAssetId == item.avatarAssetId
                    && result.isCameraOn == item.isCameraOn
        )
    }

    private class Arrangement {

        private val mapper: UICallParticipantMapper = UICallParticipantMapper(mockk(), UserTypeMapper())

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun arrange() = this to mapper
    }
}
