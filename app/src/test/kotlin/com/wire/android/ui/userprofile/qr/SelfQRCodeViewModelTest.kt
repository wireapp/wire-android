package com.wire.android.ui.userprofile.qr

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.framework.TestUser
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class SelfQRCodeViewModelTest {
    @Test
    fun `given user is on self qr code screen, then data is loaded correctly`() = runTest {
        // given
        val (_, viewModel) = Arrangement().arrange()

        // when - then
        assertEquals(
            "wire://user/${TestUser.SELF_USER.id.domain}/${TestUser.SELF_USER.id.value}",
            viewModel.selfQRCodeState.userProfileLink,
        )

        assertEquals(
            "${ServerConfig.STAGING.accounts}/user-profile/?id=${TestUser.SELF_USER.id}",
            viewModel.selfQRCodeState.userAccountProfileLink,
        )
    }

    @Test
    fun `given qr image when sharing asset then asset repository saves it and returns share uri`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withQRCodeAssetUri("content://wire/qr")
            .arrange()
        val qrImage = SelfQRCodeImage { }

        // when
        val result = viewModel.shareQRAsset(qrImage)

        // then
        assertEquals("content://wire/qr", result)
        coVerify(exactly = 1) { arrangement.qrAssetRepository.saveQRCode(qrImage) }
    }

    private class Arrangement {
        @MockK
        lateinit var selfServerConfig: SelfServerConfigUseCase

        @MockK
        lateinit var analyticsManager: AnonymousAnalyticsManager

        @MockK
        lateinit var qrAssetRepository: SelfQRCodeAssetRepository

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { selfServerConfig.invoke() } returns SelfServerConfigUseCase.Result.Success(
                serverLinks = newServerConfig(1).copy(links = ServerConfig.STAGING)
            )
        }

        fun withQRCodeAssetUri(uri: String) = apply {
            coEvery { qrAssetRepository.saveQRCode(any()) } returns uri
        }

        fun arrange() = this to SelfQRCodeViewModel(
            selfQrCodeNavArgs = SelfQrCodeNavArgs("handle", false),
            selfUserId = TestUser.SELF_USER.id,
            selfServerLinks = selfServerConfig,
            qrAssetRepository = qrAssetRepository,
            analyticsManager = analyticsManager
        )
    }
}
