package com.wire.android.feature.auth.registration.personal.ui

import com.wire.android.UnitTest
import com.wire.android.core.functional.Either
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.shouldBeUpdated
import com.wire.android.shared.user.name.NameTooShort
import com.wire.android.shared.user.name.ValidateNameParams
import com.wire.android.shared.user.name.ValidateNameUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class CreatePersonalAccountNameViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @MockK
    private lateinit var validateNameUseCase: ValidateNameUseCase

    private lateinit var nameViewModel: CreatePersonalAccountNameViewModel

    @Before
    fun setUp() {
        nameViewModel = CreatePersonalAccountNameViewModel(coroutinesTestRule.dispatcherProvider, validateNameUseCase)
    }

    @Test
    fun `given validateName() is called with a name, when use case returns success, then sets continueEnabled to true`() {
        coEvery { validateNameUseCase.run(ValidateNameParams(TEST_NAME)) } returns Either.Right(Unit)

        nameViewModel.validateName(TEST_NAME)

        nameViewModel.continueEnabled shouldBeUpdated { it shouldBe true }
        coVerify(exactly = 1) { validateNameUseCase.run(ValidateNameParams(TEST_NAME)) }
    }

    @Test
    fun `given validateName() is called with a name, when use case fails, then sets continueEnabled to false`() {
        coEvery { validateNameUseCase.run(ValidateNameParams(TEST_NAME)) } returns Either.Left(NameTooShort)

        nameViewModel.validateName(TEST_NAME)

        nameViewModel.continueEnabled shouldBeUpdated { it shouldBe false }
        coVerify(exactly = 1) { validateNameUseCase.run(ValidateNameParams(TEST_NAME)) }
    }

    companion object {
        private const val TEST_NAME = "Name Surname"
    }
}
