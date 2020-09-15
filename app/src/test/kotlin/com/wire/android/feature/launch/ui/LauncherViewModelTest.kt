package com.wire.android.feature.launch.ui

import com.wire.android.UnitTest
import com.wire.android.shared.auth.activeuser.GetActiveUserUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

class LauncherViewModelTest : UnitTest() {

    @Mock
    private lateinit var getActiveUserUseCase: GetActiveUserUseCase

    private lateinit var launcherViewModel: LauncherViewModel

    @Before
    fun setUp() {
        launcherViewModel = LauncherViewModel(getActiveUserUseCase)
    }

    @Test
    fun `given hasActiveUser is called, when use case returns true, then return true`() {
        `when`(getActiveUserUseCase.hasActiveUser()).thenReturn(true)

        assertThat(launcherViewModel.hasActiveUser()).isTrue()
    }

    @Test
    fun `given hasActiveUser is called, when use case returns false, then return false`() {
        `when`(getActiveUserUseCase.hasActiveUser()).thenReturn(false)

        assertThat(launcherViewModel.hasActiveUser()).isFalse()
    }
}
