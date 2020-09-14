package com.wire.android.shared.auth.activeuser

import com.wire.android.UnitTest
import com.wire.android.shared.activeuser.ActiveUserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

class GetActiveUserUseCaseTest : UnitTest() {

    @Mock
    private lateinit var activeUserRepository: ActiveUserRepository

    private lateinit var getActiveUserUseCase: GetActiveUserUseCase

    @Before
    fun setUp() {
        getActiveUserUseCase = GetActiveUserUseCase(activeUserRepository)
    }

    @Test
    fun `given hasActiveUser is called, when activeUserRepository returns true, then returns true`() {
        `when`(activeUserRepository.hasActiveUser()).thenReturn(true)

        assertThat(getActiveUserUseCase.hasActiveUser()).isTrue()
        verify(activeUserRepository).hasActiveUser()
    }

    @Test
    fun `given hasActiveUser is called, when activeUserRepository returns false, then returns false`() {
        `when`(activeUserRepository.hasActiveUser()).thenReturn(false)

        assertThat(getActiveUserUseCase.hasActiveUser()).isFalse()
        verify(activeUserRepository).hasActiveUser()
    }
}
