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

package com.wire.android.mdm

import android.content.Context
import android.content.Intent
import com.wire.android.services.ServicesManager
import com.wire.kalium.logic.CoreLogic
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MdmConfigurationReceiverTest {

    private val context = mockk<Context>()
    private val intent = mockk<Intent>()
    private val mdmConfigurationManager = mockk<MdmConfigurationManager>()
    private val servicesManager = mockk<ServicesManager>()
    private val coreLogic = mockk<CoreLogic>()
    
    private lateinit var receiver: MdmConfigurationReceiver

    @BeforeEach
    fun setUp() {
        receiver = MdmConfigurationReceiver().apply {
            this.mdmConfigurationManager = this@MdmConfigurationReceiverTest.mdmConfigurationManager
            this.servicesManager = this@MdmConfigurationReceiverTest.servicesManager
            this.coreLogic = this@MdmConfigurationReceiverTest.coreLogic
        }
    }

    @Test
    fun `onReceive handles APPLICATION_RESTRICTIONS_CHANGED action`() = runTest {
        every { intent.action } returns Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED
        coEvery { mdmConfigurationManager.getCertificatePinningConfig() } returns emptyMap()

        receiver.onReceive(context, intent)

        coVerify { mdmConfigurationManager.getCertificatePinningConfig() }
    }

    @Test
    fun `onReceive ignores non-restrictions actions`() = runTest {
        every { intent.action } returns "com.example.OTHER_ACTION"

        receiver.onReceive(context, intent)

        coVerify(exactly = 0) { mdmConfigurationManager.getCertificatePinningConfig() }
    }

    @Test
    fun `handleMdmConfigurationChange restarts service when config present and service running`() = runTest {
        val testConfig = mapOf("sha256/TEST" to listOf("example.com"))
        coEvery { mdmConfigurationManager.getCertificatePinningConfig() } returns testConfig
        every { servicesManager.isPersistentWebSocketServiceRunning() } returns true
        every { servicesManager.stopPersistentWebSocketService() } returns Unit
        every { intent.action } returns Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED

        receiver.onReceive(context, intent)

        // Give coroutine time to execute
        kotlinx.coroutines.delay(100)

        verify { servicesManager.stopPersistentWebSocketService() }
    }

    @Test
    fun `handleMdmConfigurationChange does not restart service when not running`() = runTest {
        val testConfig = mapOf("sha256/TEST" to listOf("example.com"))
        coEvery { mdmConfigurationManager.getCertificatePinningConfig() } returns testConfig
        every { servicesManager.isPersistentWebSocketServiceRunning() } returns false
        every { intent.action } returns Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED

        receiver.onReceive(context, intent)

        // Give coroutine time to execute
        kotlinx.coroutines.delay(100)

        verify(exactly = 0) { servicesManager.stopPersistentWebSocketService() }
    }

    @Test
    fun `handleMdmConfigurationChange handles empty config gracefully`() = runTest {
        coEvery { mdmConfigurationManager.getCertificatePinningConfig() } returns emptyMap()
        every { intent.action } returns Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED

        receiver.onReceive(context, intent)

        // Give coroutine time to execute
        kotlinx.coroutines.delay(100)

        verify(exactly = 0) { servicesManager.stopPersistentWebSocketService() }
    }
}