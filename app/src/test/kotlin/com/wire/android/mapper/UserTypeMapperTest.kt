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

package com.wire.android.mapper

import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.user.type.UserType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserTypeMapperTest {

    private val userTypeMapper = UserTypeMapper()

    @Test
    fun `given guest as a user type correctly map to guest as membership`() {
        val result = userTypeMapper.toMembership(UserType.GUEST)
        assertEquals(Membership.Guest, result)
    }

    @Test
    fun `given federated as a user type correctly map to federated as membership`() {
        val result = userTypeMapper.toMembership(UserType.FEDERATED)
        assertEquals(Membership.Federated, result)
    }

    @Test
    fun `given external as a user type correctly map to external as membership`() {
        val result = userTypeMapper.toMembership(UserType.EXTERNAL)
        assertEquals(Membership.External, result)
    }

    @Test
    fun `given internal as a user type correctly map to standard as membership`() {
        val result = userTypeMapper.toMembership(UserType.INTERNAL)
        assertEquals(Membership.Standard, result)
    }
}
