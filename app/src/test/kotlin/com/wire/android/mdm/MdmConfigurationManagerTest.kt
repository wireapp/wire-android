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
import android.content.RestrictionsManager
import android.os.Bundle
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.services.ServicesManager
import com.wire.kalium.logic.CoreLogic
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class MdmConfigurationManagerTest {

    private val context = mockk<Context>()
    private val restrictionsManager = mockk<RestrictionsManager>()
    private val json = Json.Default
    private val mdmConfigurationManager = MdmConfigurationManager(context, json)

    init {
        every { context.getSystemService(Context.RESTRICTIONS_SERVICE) } returns restrictionsManager
    }

    @Test
    fun `getCertificatePinningConfig returns empty map when no restrictions`() {
        every { restrictionsManager.applicationRestrictions } returns null

        val result = mdmConfigurationManager.getCertificatePinningConfig()

        assertEquals(emptyMap<String, List<String>>(), result)
    }

    @Test
    fun `getCertificatePinningConfig returns empty map when restrictions is empty`() {
        val emptyBundle = Bundle()
        every { restrictionsManager.applicationRestrictions } returns emptyBundle

        val result = mdmConfigurationManager.getCertificatePinningConfig()

        assertEquals(emptyMap<String, List<String>>(), result)
    }

    @Test
    fun `getCertificatePinningConfig returns parsed config when valid JSON`() {
        val bundle = Bundle().apply {
            putString("certificate_pinning_config", """{"sha256/ABC123": ["example.com", "api.example.com"]}""")
        }
        every { restrictionsManager.applicationRestrictions } returns bundle

        val result = mdmConfigurationManager.getCertificatePinningConfig()

        val expected = mapOf("sha256/ABC123" to listOf("example.com", "api.example.com"))
        assertEquals(expected, result)
    }

    @Test
    fun `getCertificatePinningConfig returns empty map when invalid JSON`() {
        val bundle = Bundle().apply {
            putString("certificate_pinning_config", "invalid json")
        }
        every { restrictionsManager.applicationRestrictions } returns bundle

        val result = mdmConfigurationManager.getCertificatePinningConfig()

        assertEquals(emptyMap<String, List<String>>(), result)
    }

    @Test
    fun `mergeCertificatePinningConfigs merges configs correctly`() {
        val defaultConfig = mapOf(
            "sha256/DEFAULT1" to listOf("default1.com"),
            "sha256/SHARED" to listOf("shared.com")
        )
        val mdmConfig = mapOf(
            "sha256/MDM1" to listOf("mdm1.com"),
            "sha256/SHARED" to listOf("shared-mdm.com")
        )

        val result = mdmConfigurationManager.mergeCertificatePinningConfigs(defaultConfig, mdmConfig)

        val expected = mapOf(
            "sha256/DEFAULT1" to listOf("default1.com"),
            "sha256/SHARED" to listOf("shared.com", "shared-mdm.com"),
            "sha256/MDM1" to listOf("mdm1.com")
        )
        assertEquals(expected, result)
    }

    @Test
    fun `mergeCertificatePinningConfigs removes duplicates`() {
        val defaultConfig = mapOf("sha256/TEST" to listOf("example.com", "duplicate.com"))
        val mdmConfig = mapOf("sha256/TEST" to listOf("duplicate.com", "new.com"))

        val result = mdmConfigurationManager.mergeCertificatePinningConfigs(defaultConfig, mdmConfig)

        val expected = mapOf("sha256/TEST" to listOf("example.com", "duplicate.com", "new.com"))
        assertEquals(expected, result)
    }
}