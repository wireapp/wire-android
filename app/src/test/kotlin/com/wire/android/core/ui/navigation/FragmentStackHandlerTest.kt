package com.wire.android.core.ui.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.wire.android.AndroidTest
import com.wire.android.feature.welcome.ui.WelcomeActivity
import com.wire.android.feature.welcome.ui.WelcomeFragment
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkClass
import io.mockk.verify
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.koin.core.qualifier.TypeQualifier
import org.koin.test.KoinTest
import org.koin.test.mock.MockProviderRule
import org.koin.test.mock.declareMock

@Ignore("A lot of wiring and complexity " +
        "for testing static methods: extension functions in this case.")
class FragmentStackHandlerTest : AndroidTest(), KoinTest {

    @get:Rule
    val mockProvider = MockProviderRule.create { clazz -> mockkClass(clazz) }

    @MockK
    private lateinit var activity: WelcomeActivity

    @MockK
    private lateinit var parentFragment: WelcomeFragment

    @MockK
    private lateinit var fragment: Fragment

    @MockK
    private lateinit var fragmentManager: FragmentManager

    @MockK
    private lateinit var fragmentTransaction: FragmentTransaction

    private lateinit var fragmentStackHandler: FragmentStackHandler

    @Before
    fun setUp() {
        every { fragmentManager.beginTransaction() } returns fragmentTransaction
        fragmentStackHandler = FragmentStackHandler()
    }

    @Test
    fun `given replaceFragment is called, when addToBackStack is true, then finds id, replaces and adds fragment to back stack`() {
        testReplaceFragment(true)
    }

    @Test
    fun `given replaceFragment is called, when addToBackStack is false, then finds id & replaces fragment without adding to back stack`() {
        testReplaceFragment(false)
    }

    private fun testReplaceFragment(addToBackStack: Boolean) {
        declareMock<FragmentContainerProvider>(qualifier = TypeQualifier(activity::class)) {
            every { getContainerResId(fragment) } returns TEST_VIEW_ID
        }
        every { activity.supportFragmentManager } returns fragmentManager
        mockReplaceFragment(addToBackStack)

        fragmentStackHandler.replaceFragment(activity, fragment, addToBackStack)

        verify(exactly = 1) { fragmentTransaction.replace(TEST_VIEW_ID, fragment) }
        if (addToBackStack) verify(exactly = 1) { fragmentTransaction.addToBackStack(TEST_FRAGMENT_TAG) }
        else verify(exactly = 0){ fragmentTransaction.addToBackStack(any()) }
    }

    private fun mockReplaceFragment(addToBackStack: Boolean) {
        every { fragmentTransaction.replace(any(), eq(fragment)) } returns fragmentTransaction
        if (addToBackStack) {
            every { fragment.tag } returns TEST_FRAGMENT_TAG
            every { fragmentTransaction.addToBackStack(TEST_FRAGMENT_TAG) } returns fragmentTransaction
        }
        every { fragmentTransaction.commit() } returns 0
    }

    @Test
    fun `given replaceChildFragment is called, when addToBackStack is true, then finds id, replaces and adds fragment to back stack`() {
        testReplaceChildFragment(true)
    }

    @Test
    fun `given replaceChildFragment is called, when addToBackStack is false, then finds id & replaces fragment without adding to stack`() {
        testReplaceChildFragment(false)
    }

    private fun testReplaceChildFragment(addToBackStack: Boolean) {
        declareMock<FragmentContainerProvider>(qualifier = TypeQualifier(parentFragment::class)) {
            every { getContainerResId(fragment) } returns TEST_VIEW_ID
        }
        every { parentFragment.childFragmentManager } returns fragmentManager
        mockReplaceFragment(addToBackStack)

        fragmentStackHandler.replaceChildFragment(parentFragment, fragment, addToBackStack)

        verify(exactly = 1) { fragmentTransaction.replace(TEST_VIEW_ID, fragment) }
        if (addToBackStack) verify (exactly = 1) { fragmentTransaction.addToBackStack(TEST_FRAGMENT_TAG) }
        else verify(exactly = 0) { fragmentTransaction.addToBackStack(any()) }
    }

    companion object {
        private const val TEST_VIEW_ID = 1234
        private const val TEST_FRAGMENT_TAG = "FragmentTag"
    }
}
