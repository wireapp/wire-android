package com.wire.android.core.ui.navigation

import androidx.fragment.app.Fragment
import com.wire.android.R
import com.wire.android.UnitTest
import io.mockk.impl.annotations.MockK
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class FragmentContainerProviderTest : UnitTest() {

    @MockK
    private lateinit var fragment: Fragment

    private lateinit var fragmentContainerProvider: FragmentContainerProvider

    @Test
    fun `given fixedProvider is called with an id, then creates an instance of FragmentContainerProvider which always returns that id`() {
        fragmentContainerProvider = FragmentContainerProvider.fixedProvider(R.id.welcomeFragmentContainer)

        fragmentContainerProvider.getContainerResId(fragment) shouldBeEqualTo R.id.welcomeFragmentContainer
    }
}
