package com.wire.android.feature.auth.registration.personal

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import com.wire.android.InstrumentationTest
import com.wire.android.R
import com.wire.android.feature.auth.registration.personal.email.CreatePersonalAccountEmailFragment
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class CreatePersonalAccountViewPagerAdapterTest : InstrumentationTest() {

    private lateinit var fragmentScenario: FragmentScenario<CreatePersonalAccountFragment>

    private lateinit var adapter: CreatePersonalAccountViewPagerAdapter

    @Before
    fun setUp() {
        fragmentScenario = launchFragment(themeResId = R.style.AppTheme_Authentication)
    }

    @Test
    fun getItemCount_returns2() {
        fragmentScenario.onFragment {
            adapter = CreatePersonalAccountViewPagerAdapter(it)
            assertThat(adapter.itemCount).isEqualTo(2)
        }
    }

    @Test
    fun createFragment_returnsEmailAndPhoneFragments() {
        fragmentScenario.onFragment {
            adapter = CreatePersonalAccountViewPagerAdapter(it)
            val firstFrag = adapter.createFragment(0)
            assertThat(firstFrag::class).isEqualTo(CreatePersonalAccountEmailFragment::class)
            //TODO: add test for Phone fragment also
        }
    }
}
