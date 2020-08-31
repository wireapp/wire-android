package com.wire.android.feature.auth.login

import com.wire.android.AndroidTest
import com.wire.android.core.network.BackendConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

class LoginViewModelTest : AndroidTest() {

    @Mock
    private lateinit var backendConfig: BackendConfig

    private lateinit var loginViewModel: LoginViewModel

    @Before
    fun setUp() {
        loginViewModel = LoginViewModel(backendConfig)
    }

    @Test
    fun forgotPasswordUri_returnsAccountsUrlWithForgotPath() {
        `when`(backendConfig.accountsUrl).thenReturn(TEST_ACCOUNTS_URL)

        assertThat(loginViewModel.forgotPasswordUri.toString()).isEqualTo("$TEST_ACCOUNTS_URL/forgot")
    }

    companion object {
        private const val TEST_ACCOUNTS_URL = "https://wire-account-url.com"
    }
}
