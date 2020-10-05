package com.wire.android.feature.auth.registration.ui.navigation

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.wire.android.AndroidTest
import com.wire.android.any
import com.wire.android.capture
import com.wire.android.core.ui.navigation.FragmentStackHandler
import com.wire.android.core.ui.navigation.UriNavigationHandler
import com.wire.android.eq
import com.wire.android.feature.auth.registration.CreateAccountActivity
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountCodeFragment
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountEmailFragment
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountNameFragment
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountPasswordFragment
import com.wire.android.feature.auth.registration.pro.email.CreateProAccountTeamEmailActivity
import com.wire.android.feature.auth.registration.pro.team.CreateProAccountTeamNameFragment
import com.wire.android.framework.android.argumentEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

class CreateAccountNavigatorTest : AndroidTest() {

    @Mock
    private lateinit var fragmentStackHandler: FragmentStackHandler

    @Mock
    private lateinit var uriNavigationHandler: UriNavigationHandler

    @Mock
    private lateinit var activity: FragmentActivity

    @Mock
    private lateinit var context: Context

    @Captor
    private lateinit var fragmentCaptor: ArgumentCaptor<Fragment>

    @Captor
    private lateinit var intentCaptor: ArgumentCaptor<Intent>

    private lateinit var createAccountNavigator: CreateAccountNavigator

    @Before
    fun setUp() {
        `when`(fragmentStackHandler.replaceFragment(any(), any(), anyBoolean())).thenReturn(0)
        createAccountNavigator = CreateAccountNavigator(fragmentStackHandler, uriNavigationHandler)
    }

    @Test
    fun `given openCreateAccount is called, then opens CreateAccountActivity`() {
        createAccountNavigator.openCreateAccount(context)

        verify(context).startActivity(capture(intentCaptor))
        intentCaptor.value.let {
            assertThat(it.component?.className).isEqualTo(CreateAccountActivity::class.java.canonicalName)
            assertThat(it.extras).isNull()
        }
    }

    @Test
    fun `given openPersonalEmailScreen is called, then replaces an instance of CreatePersonalAccountEmailFragment on given activity`() {
        createAccountNavigator.openPersonalEmailScreen(activity)

        verify(fragmentStackHandler).replaceFragment(eq(activity), capture(fragmentCaptor), eq(true))
        assertThat(fragmentCaptor.value).isInstanceOf(CreatePersonalAccountEmailFragment::class.java)
    }

    @Test
    fun `given openPersonalCodeScreen is called, then replaces an instance of CreatePersonalAccountCodeFragment with email on activity`() {
        createAccountNavigator.openPersonalCodeScreen(activity, TEST_EMAIL)

        verify(fragmentStackHandler).replaceFragment(eq(activity), capture(fragmentCaptor), eq(true))
        fragmentCaptor.value.let {
            assertThat(it).isInstanceOf(CreatePersonalAccountCodeFragment::class.java)
            assertThat(it.argumentEquals(CreatePersonalAccountCodeFragment.newInstance(TEST_EMAIL))).isTrue()
        }
    }

    @Test
    fun `given openProAccountTeamNameScreen is called, then replaces an instance of CreateProAccountTeamNameFragment on given activity`() {
        createAccountNavigator.openProTeamNameScreen(activity)

        verify(fragmentStackHandler).replaceFragment(eq(activity), capture(fragmentCaptor), eq(true))
        fragmentCaptor.value.let {
            assertThat(it).isInstanceOf(CreateProAccountTeamNameFragment::class.java)
            assertThat(it.arguments).isNull()
        }
    }

    @Test
    fun `given openPersonalNameScreen is called, then replaces an instance of CreatePersonalAccountNameFragment on given activity`() {
        createAccountNavigator.openPersonalNameScreen(activity, TEST_EMAIL, TEST_ACTIVATION_CODE)

        verify(fragmentStackHandler).replaceFragment(eq(activity), capture(fragmentCaptor), eq(true))
        fragmentCaptor.value.let {
            assertThat(it).isInstanceOf(CreatePersonalAccountNameFragment::class.java)
            assertThat(it.argumentEquals(
                CreatePersonalAccountNameFragment.newInstance(email = TEST_EMAIL, activationCode = TEST_ACTIVATION_CODE))
            ).isTrue()
        }
    }

    @Test
    fun `given openPersonalPasswordScreen is called, then replaces CreatePersonalAccountPasswordFragment on given activity`() {
        createAccountNavigator.openPersonalPasswordScreen(activity, TEST_NAME, TEST_EMAIL, TEST_ACTIVATION_CODE)

        verify(fragmentStackHandler).replaceFragment(eq(activity), capture(fragmentCaptor), eq(true))
        fragmentCaptor.value.let {
            assertThat(it).isInstanceOf(CreatePersonalAccountPasswordFragment::class.java)
            assertThat(it.argumentEquals(CreatePersonalAccountPasswordFragment.newInstance(
                name = TEST_NAME, email = TEST_EMAIL, activationCode = TEST_ACTIVATION_CODE))
            ).isTrue()
        }
    }

    @Test
    fun `given openProTeamNameScreen is called, then replaces CreatePersonalAccountPasswordFragment on given activity`() {
        createAccountNavigator.openProTeamNameScreen(activity)

        verify(fragmentStackHandler).replaceFragment(eq(activity), capture(fragmentCaptor), eq(true))
        fragmentCaptor.value.let {
            assertThat(it).isInstanceOf(CreateProAccountTeamNameFragment::class.java)
            assertThat(it.arguments).isNull()
        }
    }

    @Test
    fun `given openProTeamEmailScreen is called, then opens CreateProAccountTeamEmailActivity`() {
        createAccountNavigator.openProTeamEmailScreen(context)

        verify(context).startActivity(capture(intentCaptor))
        intentCaptor.value.let {
            assertThat(it.component?.className).isEqualTo(CreateProAccountTeamEmailActivity::class.java.canonicalName)
            assertThat(it.extras).isNull()
        }
    }

    @Test
    fun `given openProAboutTeamScreen is called, then calls uriNavigationHandler to open correct uri`() {
        createAccountNavigator.openProAboutTeamScreen(context)

        verify(uriNavigationHandler).openUri(context, "$CONFIG_URL$TEAM_ABOUT_URL_SUFFIX")
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_ACTIVATION_CODE = "234902"
        private const val TEST_NAME = "username"
        private const val CONFIG_URL = "https://wire.com"
        private const val TEAM_ABOUT_URL_SUFFIX = "/products/pro-secure-team-collaboration/"
    }
}
