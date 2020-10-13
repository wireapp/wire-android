package com.wire.android.feature.launch.ui

import com.wire.android.UnitTest
import com.wire.android.core.exception.SQLiteFailure
import com.wire.android.core.functional.Either
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.awaitValue
import com.wire.android.shared.session.usecase.CheckCurrentSessionExistsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

@ExperimentalCoroutinesApi
class LauncherViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Mock
    private lateinit var checkCurrentSessionExistsUseCase: CheckCurrentSessionExistsUseCase

    private lateinit var launcherViewModel: LauncherViewModel

    @Before
    fun setUp() {
        launcherViewModel = LauncherViewModel(coroutinesTestRule.dispatcherProvider, checkCurrentSessionExistsUseCase)
    }

    @Test
    fun `given checkIfCurrentSessionExists is called, when use case returns true, then sets true to currentSessionExistsLiveData`() =
        coroutinesTestRule.runTest {
            `when`(checkCurrentSessionExistsUseCase.run(Unit)).thenReturn(Either.Right(true))

            launcherViewModel.checkIfCurrentSessionExists()

            assertThat(launcherViewModel.currentSessionExistsLiveData.awaitValue()).isTrue()
        }

    @Test
    fun `given checkIfCurrentSessionExists is called, when use case returns false, then sets false to currentSessionExistsLiveData`() =
        coroutinesTestRule.runTest {
            `when`(checkCurrentSessionExistsUseCase.run(Unit)).thenReturn(Either.Right(false))

            launcherViewModel.checkIfCurrentSessionExists()

            assertThat(launcherViewModel.currentSessionExistsLiveData.awaitValue()).isFalse()
        }

    @Test
    fun `given checkIfCurrentSessionExists is called, when use case fails, then sets false to currentSessionExistsLiveData`() =
        coroutinesTestRule.runTest {
            `when`(checkCurrentSessionExistsUseCase.run(Unit)).thenReturn(Either.Left(SQLiteFailure()))

            launcherViewModel.checkIfCurrentSessionExists()

            assertThat(launcherViewModel.currentSessionExistsLiveData.awaitValue()).isFalse()
        }
}
