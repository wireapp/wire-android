package com.wire.android.core.ui.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.wire.android.AndroidTest
import com.wire.android.feature.welcome.ui.WelcomeActivity
import com.wire.android.feature.welcome.ui.WelcomeFragment
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.qualifier.TypeQualifier
import org.koin.test.KoinTest
import org.koin.test.mock.MockProviderRule
import org.koin.test.mock.declareMock
import org.mockito.Mock
import org.mockito.Mockito.*

class FragmentStackHandlerTest : AndroidTest(), KoinTest {

    @get:Rule
    val mockProvider = MockProviderRule.create { clazz -> mock(clazz.java) }

    @Mock
    private lateinit var activity: WelcomeActivity

    @Mock
    private lateinit var parentFragment: WelcomeFragment

    @Mock
    private lateinit var fragment: Fragment

    @Mock
    private lateinit var fragmentManager: FragmentManager

    @Mock
    private lateinit var fragmentTransaction: FragmentTransaction

    private lateinit var fragmentStackHandler: FragmentStackHandler

    @Before
    fun setUp() {
        `when`(fragmentManager.beginTransaction()).thenReturn(fragmentTransaction)
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
            `when`(this.getContainerResId(fragment)).thenReturn(TEST_VIEW_ID)
        }
        `when`(activity.supportFragmentManager).thenReturn(fragmentManager)
        mockReplaceFragment(addToBackStack)

        fragmentStackHandler.replaceFragment(activity, fragment, addToBackStack)

        verify(fragmentTransaction).replace(TEST_VIEW_ID, fragment)
        if (addToBackStack) verify(fragmentTransaction).addToBackStack(TEST_FRAGMENT_TAG)
        else verify(fragmentTransaction, never()).addToBackStack(anyString())
    }

    private fun mockReplaceFragment(addToBackStack: Boolean) {
        `when`(fragmentTransaction.replace(anyInt(), eq(fragment))).thenReturn(fragmentTransaction)
        if (addToBackStack) {
            `when`(fragment.tag).thenReturn(TEST_FRAGMENT_TAG)
            `when`(fragmentTransaction.addToBackStack(TEST_FRAGMENT_TAG)).thenReturn(fragmentTransaction)
        }
        `when`(fragmentTransaction.commit()).thenReturn(0)
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
            `when`(this.getContainerResId(fragment)).thenReturn(TEST_VIEW_ID)
        }
        `when`(parentFragment.childFragmentManager).thenReturn(fragmentManager)
        mockReplaceFragment(addToBackStack)

        fragmentStackHandler.replaceChildFragment(parentFragment, fragment, addToBackStack)

        verify(fragmentTransaction).replace(TEST_VIEW_ID, fragment)
        if (addToBackStack) verify(fragmentTransaction).addToBackStack(TEST_FRAGMENT_TAG)
        else verify(fragmentTransaction, never()).addToBackStack(anyString())
    }

    companion object {
        private const val TEST_VIEW_ID = 1234
        private const val TEST_FRAGMENT_TAG = "FragmentTag"
    }
}
