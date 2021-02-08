package com.wire.android.feature.profile.ui

import android.app.Activity
import android.content.Intent
import com.wire.android.AndroidTest
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class ProfileNavigatorTest : AndroidTest() {

    private lateinit var profileNavigator: ProfileNavigator

    @Before
    fun setUp() {
        profileNavigator = ProfileNavigator()
    }

    @Test
    fun `given openProfileScreen is called, then opens ProfileActivity`() {
        val activity = mockk<Activity>(relaxed = true)

        profileNavigator.openProfileScreen(activity)

        val intentSlot = slot<Intent>()
        verify(exactly = 1) { activity.startActivity(capture(intentSlot)) }
        intentSlot.captured.component?.className shouldBeEqualTo ProfileActivity::class.java.canonicalName
    }
}
