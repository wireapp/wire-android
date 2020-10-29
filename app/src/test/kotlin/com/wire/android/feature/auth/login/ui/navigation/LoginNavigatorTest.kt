package com.wire.android.feature.auth.login.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.wire.android.AndroidTest
import com.wire.android.core.network.BackendConfig
import com.wire.android.core.ui.navigation.UriNavigationHandler
import com.wire.android.feature.auth.login.LoginActivity
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test

class LoginNavigatorTest : AndroidTest() {

    @MockK
    private lateinit var uriNavigationHandler: UriNavigationHandler

    @MockK
    private lateinit var backendConfig: BackendConfig

    @MockK
    private lateinit var context: Context

    private lateinit var loginNavigator: LoginNavigator

    @Before
    fun setUp() {
        loginNavigator = LoginNavigator(uriNavigationHandler, backendConfig)
    }

    @Test
    fun `given openLogin is called, then opens LoginActivity`() {
        val intentSlot = slot<Intent>()
        loginNavigator.openLogin(context)

        verify(exactly = 1) { context.startActivity(capture(intentSlot)) }
        intentSlot.captured.let {
            it.component?.className shouldBe LoginActivity::class.java.canonicalName
            it.extras shouldBe null
        }
    }

    @Test
    fun `given openForgotPassword is called, then calls uriNavigationHandler to open correct uri`() {
        val uriSlot = slot<Uri>()
        every { backendConfig.accountsUrl } returns TEST_ACCOUNTS_URL

        loginNavigator.openForgotPassword(context)

        verify(exactly = 1) { uriNavigationHandler.openUri(eq(context), capture(uriSlot)) }
        uriSlot.captured.toString() shouldEqual "$TEST_ACCOUNTS_URL/forgot"
    }

    companion object {
        private const val TEST_ACCOUNTS_URL = "https://wire-account-url.com"
    }
}
