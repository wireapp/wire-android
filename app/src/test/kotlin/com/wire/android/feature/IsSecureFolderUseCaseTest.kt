/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowProcess

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class IsSecureFolderUseCaseTest {

    private val useCase = IsSecureFolderUseCase()

    @Test
    fun `given UID zero, then returns false`() {
        ShadowProcess.setUid(0)
        assertEquals(false, useCase())
    }

    @Test
    fun `given primary user UID, then returns false`() {
        ShadowProcess.setUid(10_042)
        assertEquals(false, useCase())
    }

    @Test
    fun `given secondary user UID (secure folder), then returns true`() {
        ShadowProcess.setUid(100_042)
        assertEquals(true, useCase())
    }

    @Test
    fun `given UID at exact boundary of secondary user, then returns true`() {
        ShadowProcess.setUid(100_000)
        assertEquals(true, useCase())
    }
}
