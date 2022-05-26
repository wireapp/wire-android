package com.wire.android.mapper

import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.conversation.UserType
import org.amshove.kluent.internal.assertEquals
import org.junit.Test

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
    fun `given internal as a user type correctly map to none as membership`() {
        val result = userTypeMapper.toMembership(UserType.INTERNAL)
        assertEquals(Membership.None, result)
    }

}
