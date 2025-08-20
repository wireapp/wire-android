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
package com.wire.android.ui.userprofile.service

import com.wire.kalium.logic.data.service.ServiceId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceDetailsMapperTest {

    @Test
    fun givenValidBotIdAsString_whenMappingToServiceId_thenReturnServiceId() = runTest {
        // given
        val (_, mapper) = Arrangement().arrange()

        // when
        val result = mapper.fromStringToServiceId("serviceId@providerId")

        // then
        assertEquals(
            ServiceId(id = "serviceId", provider = "providerId"),
            result
        )
    }

    @Test
    fun givenInvalidBotIdAsString_whenMappingToServiceId_thenReturnNull() = runTest {
        // given
        val (_, mapper) = Arrangement().arrange()

        // when
        val result = mapper.fromStringToServiceId("serviceId")

        // then
        assertEquals(
            null,
            result
        )
    }

    @Test
    fun givenBotIdWithInvalidDomainAsString_whenMappingToServiceId_thenReturnNull() = runTest {
        // given
        val (_, mapper) = Arrangement().arrange()

        // when
        val result = mapper.fromStringToServiceId("serviceId@providerId@extraDomain")

        // then
        assertEquals(
            null,
            result
        )
    }

    private class Arrangement {

        private val mapper: ServiceDetailsMapper = ServiceDetailsMapper()

        fun arrange() = this to mapper
    }
}
