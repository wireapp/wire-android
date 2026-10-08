/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.feature

import android.app.ActivityManager
import android.content.Intent
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.user.UserId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupAccountLimitTaskTest {
    @Test
    fun taskRemovalAllowsWarningAgainWithoutProcessRestart() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, ComponentActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        val gate = StartupAccountLimitGate()
        val userId = UserId("current", "wire.com")
        val accounts = listOf(AccountInfo.Valid(userId), AccountInfo.Valid(UserId("other", "wire.com")))
        val processId = Process.myPid()
        var originalTaskId = -1

        ActivityScenario.launch<ComponentActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                originalTaskId = activity.taskId
                assertTrue(gate.shouldShow(accounts, userId, 1, activity.taskId))
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { activity ->
                assertEquals(originalTaskId, activity.taskId)
                assertFalse(gate.shouldShow(accounts, userId, 1, activity.taskId))
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals(originalTaskId, activity.taskId)
                assertFalse(gate.shouldShow(accounts, userId, 1, activity.taskId))
                activity.finishAndRemoveTask()
            }
        }

        val activityManager = context.getSystemService(ActivityManager::class.java)
        assertTrue(activityManager.appTasks.none { it.taskInfo?.id == originalTaskId })

        ActivityScenario.launch<ComponentActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(processId, Process.myPid())
                assertNotEquals(originalTaskId, activity.taskId)
                assertTrue(gate.shouldShow(accounts, userId, 1, activity.taskId))
            }
        }
    }
}
