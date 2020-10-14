package com.wire.android.feature.auth.registration.ui.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.wire.android.AndroidTest
import com.wire.android.any
import com.wire.android.capture
import com.wire.android.core.ui.navigation.FragmentStackHandler
import com.wire.android.eq
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountCodeFragment
import com.wire.android.feature.auth.registration.personal.ui.CreatePersonalAccountEmailFragment
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
    private lateinit var activity: FragmentActivity

    @Captor
    private lateinit var fragmentCaptor: ArgumentCaptor<Fragment>

    private lateinit var createAccountNavigator: CreateAccountNavigator

    @Before
    fun setUp() {
        `when`(fragmentStackHandler.replaceFragment(any(), any(), anyBoolean())).thenReturn(0)
        createAccountNavigator = CreateAccountNavigator(fragmentStackHandler)
    }

    @Test
    fun `given openEmailScreen is called, then replaces an instance of CreatePersonalAccountEmailFragment on given activity`() {
        createAccountNavigator.openEmailScreen(activity)

        verify(fragmentStackHandler).replaceFragment(eq(activity), capture(fragmentCaptor), eq(true))
        assertThat(fragmentCaptor.value).isInstanceOf(CreatePersonalAccountEmailFragment::class.java)
    }

    @Test
    fun `given openCodeScreen is called, then replaces an instance of CreatePersonalAccountCodeFragment with email on given activity`() {
        createAccountNavigator.openCodeScreen(activity, TEST_EMAIL)

        verify(fragmentStackHandler).replaceFragment(eq(activity), capture(fragmentCaptor), eq(true))
        fragmentCaptor.value.let {
            assertThat(it).isInstanceOf(CreatePersonalAccountCodeFragment::class.java)
            assertThat(it.argumentEquals(CreatePersonalAccountCodeFragment.newInstance(TEST_EMAIL))).isTrue()
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
    }
}
