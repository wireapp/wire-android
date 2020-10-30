package com.wire.android.feature.launch.ui

import com.wire.android.UnitTest
import com.wire.android.core.exception.SQLiteFailure
import com.wire.android.core.functional.Either
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.awaitValue
import com.wire.android.shared.session.usecase.CheckCurrentSessionExistsUseCase
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class LauncherViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @MockK
    private lateinit var checkCurrentSessionExistsUseCase: CheckCurrentSessionExistsUseCase

    private lateinit var launcherViewModel: LauncherViewModel

    @Before
    fun setUp() {
        launcherViewModel = LauncherViewModel(coroutinesTestRule.dispatcherProvider, checkCurrentSessionExistsUseCase)
    }

    @Test
    fun `given checkIfCurrentSessionExists is called, when use case returns true, then sets true to currentSessionExistsLiveData`() {
        coEvery { checkCurrentSessionExistsUseCase.run(Unit) } returns Either.Right(true)

        coroutinesTestRule.runTest {
            launcherViewModel.checkIfCurrentSessionExists()

            launcherViewModel.currentSessionExistsLiveData.awaitValue() shouldEqual true
        }
    }

    @Test
    fun `given checkIfCurrentSessionExists is called, when use case returns false, then sets false to currentSessionExistsLiveData`() {
        coEvery { checkCurrentSessionExistsUseCase.run(Unit) } returns Either.Right(false)

        coroutinesTestRule.runTest {
            launcherViewModel.checkIfCurrentSessionExists()

            launcherViewModel.currentSessionExistsLiveData.awaitValue() shouldEqual false
        }
    }

    @Test
    fun `given checkIfCurrentSessionExists is called, when use case fails, then sets false to currentSessionExistsLiveData`() {
        coEvery { checkCurrentSessionExistsUseCase.run(Unit) } returns Either.Left(SQLiteFailure())

        coroutinesTestRule.runTest {
            launcherViewModel.checkIfCurrentSessionExists()

            launcherViewModel.currentSessionExistsLiveData.awaitValue() shouldEqual false
        }
    }
}
