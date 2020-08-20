package com.wire.android.feature.auth.login

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import com.wire.android.InstrumentationTest
import com.wire.android.R
import com.wire.android.feature.auth.login.email.LoginWithEmailFragment
import com.wire.android.feature.auth.registration.personal.CreatePersonalAccountFragment
import com.wire.android.feature.auth.registration.personal.CreatePersonalAccountViewPagerAdapter
import com.wire.android.feature.auth.registration.personal.email.CreatePersonalAccountEmailFragment
import org.assertj.core.api.Assertions
import org.junit.Before

import org.junit.Assert.*
import org.junit.Test

class LoginViewPagerAdapterTest : InstrumentationTest() {

    private lateinit var fragmentScenario: FragmentScenario<LoginFragment>

    private lateinit var adapter: LoginViewPagerAdapter

    @Before
    fun setUp() {
        fragmentScenario = launchFragment(themeResId = R.style.AppTheme_Authentication)
    }

    @Test
    fun getItemCount_returns2() {
        fragmentScenario.onFragment {
            adapter = LoginViewPagerAdapter(it)
            Assertions.assertThat(adapter.itemCount).isEqualTo(2)
        }
    }

    @Test
    fun createFragment_returnsEmailAndPhoneFragments() {
        fragmentScenario.onFragment {
            adapter = LoginViewPagerAdapter(it)
            val firstFrag = adapter.createFragment(0)
            Assertions.assertThat(firstFrag::class).isEqualTo(LoginWithEmailFragment::class)
            //TODO: add test for Phone fragment also
        }
    }
}
