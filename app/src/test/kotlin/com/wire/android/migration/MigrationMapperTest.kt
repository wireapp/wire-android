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
package com.wire.android.migration

import com.wire.android.config.CoroutineTestExtension
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class MigrationMapperTest {

    private lateinit var migrationMapper: MigrationMapper

    @BeforeEach
    fun setUp() {
        migrationMapper = MigrationMapper()
    }

    @Test
    fun givenEmptyStringDomain_whenMappingIdFromScala_thenUserSelfDomain() {
        val scalaId = "123-321_123"
        val domain = ""
        val selfUser = UserId("self_id", "self_domain")

        val expected = QualifiedID(scalaId, selfUser.domain)
        migrationMapper.toQualifiedId(scalaId, domain, selfUser).also {
            assertEquals(expected, it)
        }
    }

    @Test
    fun givenNonEmptyStringDomain_whenMappingIdFromScala_thenKeepTheDomainFromScala() {
        val scalaId = "123-321_123"
        val domain = "domain.com"
        val selfUser = UserId("self_id", "self_domain")

        val expected = QualifiedID(scalaId, domain)
        migrationMapper.toQualifiedId(scalaId, domain, selfUser).also {
            assertEquals(expected, it)
        }
    }
}
