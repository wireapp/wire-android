/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.sharing

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wire.android.util.getProviderAuthority
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ImportMediaAuthenticatedIntentSecurityInstrumentedTest {

    private lateinit var context: Context
    private lateinit var internalDatabaseFile: File
    private lateinit var internalAccountsFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        internalDatabaseFile = context.getDatabasePath("sharing-intent-test.db").apply {
            parentFile?.mkdirs()
            writeText("database contents")
        }
        internalAccountsFile = File(context.getDir("accounts", Context.MODE_PRIVATE), "sharing-intent/private-account.bin").apply {
            parentFile?.mkdirs()
            writeText("account contents")
        }
    }

    @After
    fun tearDown() {
        if (::internalDatabaseFile.isInitialized) {
            internalDatabaseFile.delete()
        }
        if (::internalAccountsFile.isInitialized) {
            internalAccountsFile.delete()
            internalAccountsFile.parentFile?.delete()
        }
    }

    @Test
    fun givenDatabaseFile_whenCreatingProviderUri_thenItIsNotExposed() {
        assertThrows(IllegalArgumentException::class.java) {
            FileProvider.getUriForFile(context, context.getProviderAuthority(), internalDatabaseFile)
        }
    }

    @Test
    fun givenAccountsDirFile_whenCreatingProviderUri_thenItIsNotExposed() {
        assertThrows(IllegalArgumentException::class.java) {
            FileProvider.getUriForFile(context, context.getProviderAuthority(), internalAccountsFile)
        }
    }
}
