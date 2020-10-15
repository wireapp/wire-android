package com.wire.android.feature.auth.login.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.wire.android.AndroidTest
import com.wire.android.capture
import com.wire.android.core.network.BackendConfig
import com.wire.android.core.ui.navigation.UriNavigationHandler
import com.wire.android.eq
import com.wire.android.feature.auth.login.LoginActivity
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

class LoginNavigatorTest : AndroidTest() {

    @Mock
    private lateinit var uriNavigationHandler: UriNavigationHandler

    @Mock
    private lateinit var backendConfig: BackendConfig

    @Mock
    private lateinit var context: Context

    @Captor
    private lateinit var intentCaptor: ArgumentCaptor<Intent>

    @Captor
    private lateinit var uriCaptor: ArgumentCaptor<Uri>

    private lateinit var loginNavigator: LoginNavigator

    @Before
    fun setUp() {
        loginNavigator = LoginNavigator(uriNavigationHandler, backendConfig)
    }

    @Test
    fun `given openLogin is called, then opens LoginActivity`() {
        loginNavigator.openLogin(context)

        verify(context).startActivity(capture(intentCaptor))
        intentCaptor.value.let {
            assertThat(it.component?.className).isEqualTo(LoginActivity::class.java.canonicalName)
            assertThat(it.extras).isNull()
        }
    }

    @Test
    fun `given openForgotPassword is called, then calls uriNavigationHandler to open correct uri`() {
        `when`(backendConfig.accountsUrl).thenReturn(TEST_ACCOUNTS_URL)

        loginNavigator.openForgotPassword(context)

        verify(uriNavigationHandler).openUri(eq(context), capture(uriCaptor))
        assertThat(uriCaptor.value.toString()).isEqualTo("$TEST_ACCOUNTS_URL/forgot")
    }

    companion object {
        private const val TEST_ACCOUNTS_URL = "https://wire-account-url.com"
    }
}
