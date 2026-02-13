package com.wire.android.ui.userprofile.qr

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.framework.TestUser
import com.ramcosta.composedestinations.generated.app.navArgs
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
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

    private class Arrangement {
        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var selfServerConfig: SelfServerConfigUseCase

        @MockK
        lateinit var analyticsManager: AnonymousAnalyticsManager

        val context = mockk<Context>()

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { selfServerConfig.invoke() } returns SelfServerConfigUseCase.Result.Success(
                serverLinks = newServerConfig(1).copy(links = ServerConfig.STAGING)
            )
            every { savedStateHandle.navArgs<SelfQrCodeNavArgs>() } returns SelfQrCodeNavArgs("handle", false)
        }

        fun arrange() = this to SelfQRCodeViewModel(
            savedStateHandle = savedStateHandle,
            context = context,
            selfUserId = TestUser.SELF_USER.id,
            selfServerLinks = selfServerConfig,
            kaliumFileSystem = fakeKaliumFileSystem,
            dispatchers = TestDispatcherProvider(),
            analyticsManager = analyticsManager
        )

        val fakeKaliumFileSystem: FakeKaliumFileSystem = FakeKaliumFileSystem()
    }
}
