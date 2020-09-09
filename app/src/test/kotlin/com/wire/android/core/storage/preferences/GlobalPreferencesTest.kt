package com.wire.android.core.storage.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.wire.android.AndroidTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString

class GlobalPreferencesTest : AndroidTest() {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var globalPreferences: GlobalPreferences

    @Before
    fun setUp() {
        sharedPreferences = context().getSharedPreferences(TEST_PREF_NAME, Context.MODE_PRIVATE)

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)

        globalPreferences = GlobalPreferences(mockContext)
    }

    @After
    fun tearDown() {
        sharedPreferences.edit(commit = true) { clear() }
    }

    @Test
    fun `given activeUserId is called, when sharedPref doesn't have a value for "activeUser", then returns null`() {
        sharedPreferences.edit(commit = true) { putString(ACTIVE_USER_ID_PREF_KEY, null) }

        assertThat(globalPreferences.activeUserId).isNull()
    }

    @Test
    fun `given activeUserId is called, when sharedPref doesn't have a value for "activeUser", then returns that value`() {
        sharedPreferences.edit(commit = true) { putString(ACTIVE_USER_ID_PREF_KEY, TEST_USER_ID_PREF_VALUE) }

        assertThat(globalPreferences.activeUserId).isEqualTo(TEST_USER_ID_PREF_VALUE)
    }

    @Test
    fun `given activeUserId is set, then sets the value for "activeUser"`() {
        val previousId = sharedPreferences.getString(ACTIVE_USER_ID_PREF_KEY, null)
        assertThat(previousId).isNull()

        globalPreferences.activeUserId = TEST_USER_ID_PREF_VALUE

        val newId = sharedPreferences.getString(ACTIVE_USER_ID_PREF_KEY, null)
        assertThat(newId).isEqualTo(TEST_USER_ID_PREF_VALUE)
    }

    companion object {
        private const val TEST_PREF_NAME = "GlobalPreferencesTestPref"
        private const val ACTIVE_USER_ID_PREF_KEY = "activeUser"
        private const val TEST_USER_ID_PREF_VALUE = "324034-sdfk324-324kl"
    }
}
