package com.wire.android.feature.launch.ui

import com.wire.android.UnitTest
import com.wire.android.core.exception.SQLiteFailure
import com.wire.android.core.functional.Either
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.awaitValue
import com.wire.android.shared.session.usecase.HasCurrentSessionUseCase
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
    private lateinit var hasCurrentSessionUseCase: HasCurrentSessionUseCase

    private lateinit var launcherViewModel: LauncherViewModel

    @Before
    fun setUp() {
        launcherViewModel = LauncherViewModel(coroutinesTestRule.dispatcherProvider, hasCurrentSessionUseCase)
    }

    @Test
    fun `given checkCurrentSessionExists is called, when use case returns true w success, then sets true to hasCurrentSessionLiveData`() =
        coroutinesTestRule.runTest {
            `when`(hasCurrentSessionUseCase.run(Unit)).thenReturn(Either.Right(true))

            launcherViewModel.checkCurrentSessionExists()

            assertThat(launcherViewModel.hasCurrentSessionLiveData.awaitValue()).isTrue()
        }

    @Test
    fun `given checkCurrentSessionExists is called, when use case returns false w success, then sets false to hasCurrentSessionLiveData`() =
        coroutinesTestRule.runTest {
            `when`(hasCurrentSessionUseCase.run(Unit)).thenReturn(Either.Right(false))

            launcherViewModel.checkCurrentSessionExists()

            assertThat(launcherViewModel.hasCurrentSessionLiveData.awaitValue()).isFalse()
        }

    @Test
    fun `given checkCurrentSessionExists is called, when use case fails, then sets false to hasCurrentSessionLiveData`() =
        coroutinesTestRule.runTest {
            `when`(hasCurrentSessionUseCase.run(Unit)).thenReturn(Either.Left(SQLiteFailure()))

            launcherViewModel.checkCurrentSessionExists()

            assertThat(launcherViewModel.hasCurrentSessionLiveData.awaitValue()).isFalse()
        }
}
