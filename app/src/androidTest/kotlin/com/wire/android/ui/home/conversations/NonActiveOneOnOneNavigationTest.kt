/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.home.conversations

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.ui.WireActivity
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Test

/**
 * Runs against the account already logged in on the device.
 * Pass the id of a one-on-one conversation that is not the other user's active one-on-one:
 * `-e nonActiveOneOnOneId <uuid>@<domain>`.
 */
class NonActiveOneOnOneNavigationTest {

    @Test
    fun givenNonActiveOneOnOne_whenOpened_thenRedirectsToActiveConversationOnce() {
        val conversationId = InstrumentationRegistry.getArguments().getString(ARG_CONVERSATION_ID)
        assumeNotNull(conversationId)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("wire://conversation/$conversationId"))
            .setClass(context, WireActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val marker = "$MARKER_PREFIX-${System.nanoTime()}"
        Log.i(MARKER_TAG, marker)

        ActivityScenario.launch<WireActivity>(intent).use {
            Thread.sleep(SETTLE_MILLIS)
            val conversationScreens = routeLogsAfter(marker).count { it.contains(CONVERSATION_ROUTE) }

            assertTrue(
                "Expected the non-active conversation and then its active replacement, but the conversation route " +
                    "was navigated to $conversationScreens times in ${SETTLE_MILLIS}ms",
                conversationScreens <= MAX_CONVERSATION_ROUTES
            )
        }
    }

    private fun routeLogsAfter(marker: String): List<String> {
        val process = ProcessBuilder("logcat", "-d", "-v", "brief").redirectErrorStream(true).start()
        val lines = process.inputStream.bufferedReader().readLines()
        return lines.dropWhile { !it.contains(marker) }.filter { it.contains("WireActivity graph route=") }
    }

    private companion object {
        const val ARG_CONVERSATION_ID = "nonActiveOneOnOneId"
        const val MARKER_TAG = "NonActiveOneOnOneTest"
        const val MARKER_PREFIX = "non-active-one-on-one-marker"
        const val CONVERSATION_ROUTE = "route=app/conversation_screen"
        const val SETTLE_MILLIS = 6_000L
        const val MAX_CONVERSATION_ROUTES = 2
    }
}
