/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.runtime

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.wire.android.navigation.routes.auth.AuthenticationLoginArguments
import com.wire.android.navigation.routes.auth.AuthenticationTeamAccountCreationRequest
import com.wire.android.navigation.routes.auth.NewLoginPasswordRoute
import com.wire.android.ui.WireActivity
import com.wire.navigation.WireBackStackMode
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class WireNavigation3ActivityEffectsAndroidTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun givenFeedbackIsRequested_whenBuildingIntent_thenLegacyEmailChooserPayloadIsPreserved() {
        val chooser = buildWireNavigation3FeedbackIntent(context)
        val emailIntent = checkNotNull(
            chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        )

        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertEquals(Intent.ACTION_SEND, emailIntent.action)
        assertArrayEquals(
            arrayOf("wire-newandroid-feedback@wearezeta.zendesk.com"),
            emailIntent.getStringArrayExtra(Intent.EXTRA_EMAIL),
        )
        assertEquals("Feedback - Wire Beta", emailIntent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals(Intent.ACTION_SENDTO, emailIntent.selector?.action)
        assertEquals("mailto:", emailIntent.selector?.dataString)
        assertTrue(emailIntent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().isNotBlank())
    }

    @Test
    fun givenTeamAccountWebRequest_whenBuildingLaunch_thenUrlAndUpdateExistingReturnArePreserved() {
        val returnRoute = NewLoginPasswordRoute(
            args = AuthenticationLoginArguments(),
            flowId = "create-account-flow",
        )
        val launch = buildWireNavigation3TeamAccountWebLaunch(
            context,
            AuthenticationTeamAccountCreationRequest(
                url = "https://wire.test/register",
                returnRoute = returnRoute,
            ),
        )

        assertEquals("https://wire.test/register", launch.intent.dataString)
        assertSame(returnRoute, launch.returnRoute)
        assertEquals(returnRoute, launch.returnCommand().destination)
        assertEquals(WireBackStackMode.UPDATE_EXISTING, launch.returnCommand().backStackMode)
    }

    @Test
    fun givenLogoutRestart_whenBuildingIntent_thenWireActivityReplacesTheTask() {
        val intent = buildWireNavigation3RestartIntent(context)

        assertEquals(WireActivity::class.java.name, intent.component?.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
    }
}
