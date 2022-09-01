package com.wire.android.mapper

import com.wire.kalium.logic.data.call.Participant
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserAssetId
import io.mockk.MockKAnnotations
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class UICallParticipantMapperTest {

    @Test
    fun givenParticipant_whenMappingToUICallParticipant_thenCorrectValuesShouldBeReturned() = runTest {
        val (arrangement, mapper) = Arrangement().arrange()
        // Given
        val item = Participant(
            QualifiedID("idvalue", "iddomain"),
            "clientId",
            "name",
            false,
            false,
            false,
            false,
            UserAssetId("assetvalue", "assetdomain")
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
