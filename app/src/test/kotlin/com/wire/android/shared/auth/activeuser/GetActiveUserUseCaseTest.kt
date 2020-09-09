package com.wire.android.shared.auth.activeuser

import com.wire.android.UnitTest
import com.wire.android.shared.activeusers.ActiveUsersRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

class GetActiveUserUseCaseTest : UnitTest() {

    @Mock
    private lateinit var activeUsersRepository: ActiveUsersRepository

    private lateinit var getActiveUserUseCase: GetActiveUserUseCase

    @Before
    fun setUp() {
        getActiveUserUseCase = GetActiveUserUseCase(activeUsersRepository)
    }

    @Test
    fun `given hasActiveUser is called, when activeUsersRepo returns true, then returns true`() {
        `when`(activeUsersRepository.hasActiveUser()).thenReturn(true)

        assertThat(getActiveUserUseCase.hasActiveUser()).isTrue()
        verify(activeUsersRepository).hasActiveUser()
    }

    @Test
    fun `given hasActiveUser is called, when activeUsersRepo returns false, then returns false`() {
        `when`(activeUsersRepository.hasActiveUser()).thenReturn(false)

        assertThat(getActiveUserUseCase.hasActiveUser()).isFalse()
        verify(activeUsersRepository).hasActiveUser()
    }
}
