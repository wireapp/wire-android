package com.wire.android.feature.auth.registration.personal

import androidx.fragment.app.FragmentManager
import com.wire.android.UnitTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

class CreatePersonalAccountViewPagerAdapterTest : UnitTest() {

    private lateinit var createPersonalAccountViewPagerAdapter: CreatePersonalAccountViewPagerAdapter

    @Mock
    private lateinit var fragmentManager: FragmentManager

    @Mock
    private lateinit var titles: List<String>

    @Before
    fun setup() {
        createPersonalAccountViewPagerAdapter = CreatePersonalAccountViewPagerAdapter(fragmentManager, titles)
    }

    @Test
    fun `given getCount() is called, then return the size of the adapter`() {
        `when`(titles.size).thenReturn(TEST_SIZE)
        assertEquals(TEST_SIZE, createPersonalAccountViewPagerAdapter.count)
    }

    @Test
    fun `given getPageTitle(position) is called, then return the title to show for the given position`() {
        `when`(titles[0]).thenReturn(TEST_TITLE_EMAIL)
        `when`(titles[1]).thenReturn(TEST_TITLE_PHONE)

        assertEquals(TEST_TITLE_EMAIL.toUpperCase(), createPersonalAccountViewPagerAdapter.getPageTitle(0))
        assertEquals(TEST_TITLE_PHONE.toUpperCase(), createPersonalAccountViewPagerAdapter.getPageTitle(1))
    }

    companion object {
        private const val TEST_SIZE = 2
        private const val TEST_TITLE_EMAIL = "email"
        private const val TEST_TITLE_PHONE = "phone"
    }
}
