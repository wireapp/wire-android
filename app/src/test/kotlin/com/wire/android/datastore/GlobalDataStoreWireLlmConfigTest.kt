/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GlobalDataStoreWireLlmConfigTest {
    private lateinit var globalDataStore: GlobalDataStore

    @BeforeEach
    fun setUp() = runTest {
        val context: Context = ApplicationProvider.getApplicationContext()
        globalDataStore = GlobalDataStore(context)
        globalDataStore.clear()
    }

    @Test
    fun givenServerIpIsStored_whenObserving_thenValueIsReturned() = runTest {
        globalDataStore.setWireLlmServerIp("192.168.1.20")

        assertEquals("192.168.1.20", globalDataStore.observeWireLlmServerIp().first())
    }
}
