package com.wire.android.core.network.auth.accesstoken

import com.wire.android.UnitTest
import com.wire.android.core.extension.EMPTY
import com.wire.android.shared.session.Session
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.Before
import org.junit.Test

class AuthenticationManagerTest : UnitTest() {

    @MockK
    private lateinit var session: Session

    private lateinit var authenticationManager: AuthenticationManager

    @Before
    fun setup() {
        authenticationManager = AuthenticationManager()
    }

    @Test
    fun `given authorizationToken is called, when session holds a tokenType and access Token, then returns a authorization token`() {
        val tokenType = "Bearer"
        val accessToken = "114512"
        every { session.tokenType } returns tokenType
        every { session.accessToken } returns accessToken

        val result = authenticationManager.authorizationToken(session)

        result shouldBeEqualTo "$tokenType $accessToken"
    }

    @Test
    fun `given authorizationToken is called, when tokenType and access Token are empty, then returns an empty authorization token`() {
        every { session.tokenType } returns String.EMPTY
        every { session.accessToken } returns String.EMPTY

        val result = authenticationManager.authorizationToken(session)

        result shouldNotBeEqualTo String.EMPTY
    }
}
