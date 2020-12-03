package com.wire.android.feature.auth.registration.ui.navigation

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.wire.android.AndroidTest
import com.wire.android.core.ui.navigation.FragmentStackHandler
import com.wire.android.core.ui.navigation.UriNavigationHandler
import com.wire.android.feature.auth.registration.CreateAccountActivity
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountCodeFragment
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountEmailFragment
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountNameFragment
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountPasswordFragment
import com.wire.android.feature.auth.registration.pro.email.CreateProAccountTeamEmailFragment
import com.wire.android.feature.auth.registration.pro.team.CreateProAccountTeamNameFragment
import com.wire.android.framework.android.argumentEquals
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class CreateAccountNavigatorTest : AndroidTest() {

    @MockK
    private lateinit var fragmentStackHandler: FragmentStackHandler

    @MockK
    private lateinit var uriNavigationHandler: UriNavigationHandler

    @MockK
    private lateinit var activity: FragmentActivity

    @MockK
    private lateinit var context: Context

    private lateinit var fragmentSlot: CapturingSlot<Fragment>

    private lateinit var intentSlot: CapturingSlot<Intent>

    private lateinit var createAccountNavigator: CreateAccountNavigator

    @Before
    fun setUp() {
        fragmentSlot = slot()
        intentSlot = slot()
        every { fragmentStackHandler.replaceFragment(any(), any(), any()) } returns 0
        createAccountNavigator = CreateAccountNavigator(fragmentStackHandler, uriNavigationHandler)
    }

    @Test
    fun `given openCreateAccount is called, then opens CreateAccountActivity`() {
        createAccountNavigator.openCreateAccount(context)

        verify(exactly = 1) { context.startActivity(capture(intentSlot)) }
        intentSlot.captured.let {
            it.component?.className shouldBeEqualTo CreateAccountActivity::class.java.canonicalName
            it.extras shouldBe null
        }
    }

    @Test
    fun `given openPersonalAccountEmailScreen is called, then replaces an instance of CreatePersonalAccountEmailFragment on activity`() {
        createAccountNavigator.openPersonalAccountEmailScreen(activity)

        verify(exactly = 1) { fragmentStackHandler.replaceFragment(activity, capture(fragmentSlot), true) }
        fragmentSlot.captured shouldBeInstanceOf CreatePersonalAccountEmailFragment::class.java
    }

    @Test
    fun `given openPersonalAccountCodeScreen is called, then replaces an instance of CreatePersonalAccountCodeFragment on activity`() {
        createAccountNavigator.openPersonalAccountCodeScreen(activity, TEST_EMAIL)

        verify(exactly = 1) { fragmentStackHandler.replaceFragment(activity, capture(fragmentSlot), true) }
        fragmentSlot.captured.let {
            it shouldBeInstanceOf CreatePersonalAccountCodeFragment::class.java
            it.argumentEquals(CreatePersonalAccountCodeFragment.newInstance(TEST_EMAIL)) shouldBe true
        }
    }

    @Test
    fun `given openProAccountTeamNameScreen is called, then replaces an instance of CreateProAccountTeamNameFragment on given activity`() {
        createAccountNavigator.openProAccountTeamNameScreen(activity)

        verify(exactly = 1) { fragmentStackHandler.replaceFragment(activity, capture(fragmentSlot), true) }
        fragmentSlot.captured.let {
            it shouldBeInstanceOf CreateProAccountTeamNameFragment::class.java
            it.arguments shouldBe null
        }
    }

    @Test
    fun `given openPersonalAccountNameScreen is called, then replaces an instance of CreatePersonalAccountNameFragment on activity`() {
        createAccountNavigator.openPersonalAccountNameScreen(activity, TEST_EMAIL, TEST_ACTIVATION_CODE)

        verify(exactly = 1) { fragmentStackHandler.replaceFragment(activity, capture(fragmentSlot), true) }
        fragmentSlot.captured.let {
            it shouldBeInstanceOf CreatePersonalAccountNameFragment::class.java
            it.argumentEquals(
                CreatePersonalAccountNameFragment.newInstance(email = TEST_EMAIL, activationCode = TEST_ACTIVATION_CODE)
            ) shouldBe true
        }
    }

    @Test
    fun `given openPersonalAccountPasswordScreen is called, then replaces CreatePersonalAccountPasswordFragment on given activity`() {
        createAccountNavigator.openPersonalAccountPasswordScreen(activity, TEST_NAME, TEST_EMAIL, TEST_ACTIVATION_CODE)

        verify(exactly = 1) { fragmentStackHandler.replaceFragment(activity, capture(fragmentSlot), true) }
        fragmentSlot.captured.let {
            it shouldBeInstanceOf CreatePersonalAccountPasswordFragment::class.java
            it.argumentEquals(
                CreatePersonalAccountPasswordFragment.newInstance(
                    name = TEST_NAME, email = TEST_EMAIL, activationCode = TEST_ACTIVATION_CODE
                )
            ) shouldBe true
        }
    }

    @Test
    fun `given openProAccountTeamNameScreen is called, then replaces CreatePersonalAccountPasswordFragment on given activity`() {
        createAccountNavigator.openProAccountTeamNameScreen(activity)

        verify(exactly = 1) { fragmentStackHandler.replaceFragment(activity, capture(fragmentSlot), true) }
        fragmentSlot.captured.let {
            it shouldBeInstanceOf CreateProAccountTeamNameFragment::class.java
            it.arguments shouldBe null
        }
    }

    @Test
    fun `given openProAccountTeamEmailScreen is called, then opens CreateProAccountTeamEmailFragment`() {
        createAccountNavigator.openProAccountTeamEmailScreen(activity)

        verify(exactly = 1) { fragmentStackHandler.replaceFragment(activity, capture(fragmentSlot), true) }
        fragmentSlot.captured.let {
            it shouldBeInstanceOf CreateProAccountTeamEmailFragment::class.java
            it.arguments shouldBe null
        }
    }

    @Test
    fun `given openProAccountAboutTeamScreen is called, then calls uriNavigationHandler to open correct uri`() {
        createAccountNavigator.openProAccountAboutTeamScreen(context)

        verify(exactly = 1) { uriNavigationHandler.openUri(context, "$CONFIG_URL$TEAM_ABOUT_URL_SUFFIX") }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_ACTIVATION_CODE = "234902"
        private const val TEST_NAME = "username"
        private const val CONFIG_URL = "https://wire.com"
        private const val TEAM_ABOUT_URL_SUFFIX = "/products/pro-secure-team-collaboration/"
    }
}
