package com.wire.android.ui.userprofile.qr

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.framework.TestUser
import com.wire.android.util.newServerConfig
import com.wire.content.external.ExternalContentReference
import com.wire.content.external.PlatformResult
import com.wire.content.media.EncodedImage
import com.wire.content.media.EncodedImageExporter
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
    fun `given an encoded qr image, when sharing, then it is exported through the platform boundary`() = runTest {
        val reference = ExternalContentReference("content://qr")
        val (arrangement, viewModel) = Arrangement().apply {
            coEvery { encodedImageExporter.export(any()) } returns PlatformResult.Success(reference)
        }.arrange()

        val result = viewModel.shareQRAsset(EncodedImage(byteArrayOf(1, 2, 3), "image/jpeg"))

        assertEquals(PlatformResult.Success(reference), result)
        coVerify(exactly = 1) {
            arrangement.encodedImageExporter.export(
                match {
                    it.displayName == SelfQRCodeViewModel.TEMP_SELF_QR_FILENAME &&
                        it.image.bytes.contentEquals(byteArrayOf(1, 2, 3))
                }
            )
        }
    }

    private class Arrangement {
        @MockK
        lateinit var selfServerConfig: SelfServerConfigUseCase

        @MockK
        lateinit var analyticsManager: AnonymousAnalyticsManager

        @MockK
        lateinit var encodedImageExporter: EncodedImageExporter

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { selfServerConfig.invoke() } returns SelfServerConfigUseCase.Result.Success(
                serverLinks = newServerConfig(1).copy(links = ServerConfig.STAGING)
            )
        }

        fun arrange() = this to SelfQRCodeViewModel(
            navigationArgs = SelfQrCodeViewModelArgs("handle", false),
            selfUserId = TestUser.SELF_USER.id,
            selfServerLinks = selfServerConfig,
            kaliumFileSystem = fakeKaliumFileSystem,
            encodedImageExporter = encodedImageExporter,
            analyticsManager = analyticsManager
        )

        val fakeKaliumFileSystem: FakeKaliumFileSystem = FakeKaliumFileSystem()
    }
}
