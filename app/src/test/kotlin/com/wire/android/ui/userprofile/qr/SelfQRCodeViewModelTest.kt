package com.wire.android.ui.userprofile.qr

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestUser
import com.wire.android.ui.navArgs
import com.wire.android.ui.userprofile.image.AvatarPickerViewModelTest.Companion.fakeKaliumFileSystem
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class SelfQRCodeViewModelTest {
    @Test
    fun `given user is on self qr code screen, then data is loaded correctly`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement().arrange()

        // when - then
        assertEquals(
            expected = "https://wire-account-staging.zinfra.io/user-profile/?id=${TestUser.SELF_USER.id.value}",
            actual = viewModel.selfQRCodeState.userProfileLink,
        )
    }

    private class Arrangement {
        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var selfServerConfig: SelfServerConfigUseCase

        val context = mockk<Context>()

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { selfServerConfig.invoke() } returns SelfServerConfigUseCase.Result.Success(
                serverLinks = newServerConfig(1).copy(links = ServerConfig.STAGING)
            )
            every { savedStateHandle.navArgs<SelfQrCodeNavArgs>() } returns SelfQrCodeNavArgs("handle")
        }

        fun arrange() = this to SelfQRCodeViewModel(
            savedStateHandle = savedStateHandle,
            context = context,
            selfUserId = TestUser.SELF_USER.id,
            selfServerLinks = selfServerConfig,
            kaliumFileSystem = fakeKaliumFileSystem,
            dispatchers = TestDispatcherProvider()
        )
    }
}
