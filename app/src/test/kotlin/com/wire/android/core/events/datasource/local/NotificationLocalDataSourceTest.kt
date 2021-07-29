package com.wire.android.core.events.datasource.local

import android.content.SharedPreferences
import com.wire.android.UnitTest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class NotificationLocalDataSourceTest : UnitTest() {

    @MockK
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var notificationLocalDataSource: NotificationLocalDataSource

    @Before
    fun setUp() {
        notificationLocalDataSource = NotificationLocalDataSource(sharedPreferences)
    }

    @Test
    fun `given lastNotificationId is called, there is a notification id in shared preferences, then returns the id`() {
        every { sharedPreferences.getString(any(), any()) } returns LAST_NOTIFICATION_ID

        val result = notificationLocalDataSource.lastNotificationId()

        result shouldBeEqualTo LAST_NOTIFICATION_ID
    }

    @Test
    fun `given lastNotificationId is called, there is no notification id in shared preferences, then returns null`() {
        every { sharedPreferences.getString(any(), any()) } returns null

        val result = notificationLocalDataSource.lastNotificationId()

        result shouldBeEqualTo null
    }

    @Test
    fun `given saveLastNotificationId is called, when passed notificationId is valid, then writes the id to shared preferences`() {
        val editor = mockk<SharedPreferences.Editor>(relaxUnitFun = true)
        every { editor.putString(any(), any()) } returns editor
        every { sharedPreferences.edit() } returns editor

        notificationLocalDataSource.saveLastNotificationId(LAST_NOTIFICATION_ID)

        verify(exactly = 1) { editor.putString(LAST_NOTIFICATION_ID_KEY, LAST_NOTIFICATION_ID) }
    }

    companion object {
        private const val LAST_NOTIFICATION_ID_KEY = "last_notification_id"
        private const val LAST_NOTIFICATION_ID = "1234567890"
    }
}
