package com.wire.android.core.ui.navigation

import androidx.fragment.app.Fragment
import com.wire.android.R
import com.wire.android.UnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mock

class FragmentContainerProviderTest : UnitTest() {

    @Mock
    private lateinit var fragment: Fragment

    private lateinit var fragmentContainerProvider: FragmentContainerProvider

    @Test
    fun `given fixedProvider is called with an id, then creates an instance of FragmentContainerProvider which always returns that id`() {
        fragmentContainerProvider = FragmentContainerProvider.fixedProvider(R.id.welcomeFragmentContainer)

        assertThat(fragmentContainerProvider.getContainerResId(fragment)).isEqualTo(R.id.welcomeFragmentContainer)
    }
}
