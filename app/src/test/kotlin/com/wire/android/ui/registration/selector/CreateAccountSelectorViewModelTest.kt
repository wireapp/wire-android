package com.wire.android.ui.registration.selector

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.kalium.logic.configuration.server.ServerConfig
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class CreateAccountSelectorViewModelTest {

    @Test
    fun `given view model is initialized, then server config`() = runTest {
        val (_, createAccountSelectorViewModel) = Arrangement().arrange()

        assertEquals(ServerConfig.STAGING, createAccountSelectorViewModel.serverConfig)
    }

    @Test
    fun `given view model is initialized, then get email if there is one from nav args`() = runTest {
        val expectedEmail = "alice@mail.com"
        val (_, createAccountSelectorViewModel) = Arrangement().withEmailNavArgs(expectedEmail).arrange()

        assertEquals(expectedEmail, createAccountSelectorViewModel.email)
    }

    @Test
    fun `given view model is initialized, then load teams url from server config`() = runTest {
        val (_, createAccountSelectorViewModel) = Arrangement().arrange()

        assertEquals(ServerConfig.STAGING.teams, createAccountSelectorViewModel.teamAccountCreationUrl)
    }

    private class Arrangement {

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        private var navArgs = CreateAccountSelectorNavArgs(ServerConfig.STAGING)

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withEmailNavArgs(email: String) = apply {
            navArgs = CreateAccountSelectorNavArgs(ServerConfig.STAGING, email)
        }

        fun arrange() = this to CreateAccountSelectorViewModel(navArgs, globalDataStore, ServerConfig.STAGING)
    }
}
