package com.wire.android.feature.auth.registration.pro.team

import com.wire.android.UnitTest
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.registration.pro.team.usecase.GetTeamNameUseCase
import com.wire.android.feature.auth.registration.pro.team.usecase.UpdateTeamNameUseCase
import com.wire.android.framework.livedata.shouldBeUpdated
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class CreateProAccountTeamNameViewModelTest : UnitTest() {

    private lateinit var viewModel: CreateProAccountTeamNameViewModel

    @MockK
    private lateinit var getTeamNameUseCase: GetTeamNameUseCase

    @MockK
    private lateinit var updateTeamNameUseCase: UpdateTeamNameUseCase

    @Before
    fun setup() {
        coEvery { getTeamNameUseCase.run(Unit) } returns Either.Right(TEST_TEAM_NAME)
        viewModel = CreateProAccountTeamNameViewModel(getTeamNameUseCase, updateTeamNameUseCase)
    }

    @Test
    fun `given viewModel is initialised, when teamName is available, propagate teamName up to the view`() {
        viewModel.teamNameLiveData shouldBeUpdated { it shouldBeEqualTo TEST_TEAM_NAME }
    }

    @Test
    fun `given empty team name, when on team name text is changed, confirmation button should be disabled`() {
        viewModel.onTeamNameTextChanged(String.EMPTY)

        viewModel.confirmationButtonEnabled shouldBeUpdated { it shouldBe false }
    }

    @Test
    fun `given empty team name, when on team name text is changed, confirmation button should be enabled`() {
        viewModel.onTeamNameTextChanged(TEST_TEAM_NAME)

        viewModel.confirmationButtonEnabled shouldBeUpdated { it shouldBe true }
    }

    companion object {
        private const val TEST_TEAM_NAME = "teamName"
    }
}
