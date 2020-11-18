package com.wire.android.feature.launch.ui

import com.wire.android.UnitTest
import com.wire.android.core.exception.SQLiteFailure
import com.wire.android.core.functional.Either
import com.wire.android.framework.livedata.shouldBeUpdated
import com.wire.android.shared.session.usecase.CheckCurrentSessionExistsUseCase
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class LauncherViewModelTest : UnitTest() {

    @MockK
    private lateinit var checkCurrentSessionExistsUseCase: CheckCurrentSessionExistsUseCase

    private lateinit var launcherViewModel: LauncherViewModel

    @Before
    fun setUp() {
        launcherViewModel = LauncherViewModel(checkCurrentSessionExistsUseCase)
    }

    @Test
    fun `given checkIfCurrentSessionExists is called, when use case returns true, then sets true to currentSessionExistsLiveData`() {
        coEvery { checkCurrentSessionExistsUseCase.run(Unit) } returns Either.Right(true)

        launcherViewModel.checkIfCurrentSessionExists()

        launcherViewModel.currentSessionExistsLiveData shouldBeUpdated { it shouldBe true }
    }

    @Test
    fun `given checkIfCurrentSessionExists is called, when use case returns false, then sets false to currentSessionExistsLiveData`() {
        coEvery { checkCurrentSessionExistsUseCase.run(Unit) } returns Either.Right(false)

        launcherViewModel.checkIfCurrentSessionExists()

        launcherViewModel.currentSessionExistsLiveData shouldBeUpdated { it shouldBe false }
    }

    @Test
    fun `given checkIfCurrentSessionExists is called, when use case fails, then sets false to currentSessionExistsLiveData`() {
        coEvery { checkCurrentSessionExistsUseCase.run(Unit) } returns Either.Left(SQLiteFailure())

        launcherViewModel.checkIfCurrentSessionExists()

        launcherViewModel.currentSessionExistsLiveData shouldBeUpdated { it shouldBe false }
    }
}
