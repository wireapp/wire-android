package com.wire.android.feature.auth.registration.pro.team

import com.wire.android.UnitTest
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.registration.pro.team.usecase.GetTeamNameUseCase
import com.wire.android.feature.auth.registration.pro.team.usecase.UpdateTeamNameUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.awaitValue
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class CreateProAccountTeamNameViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    private lateinit var viewModel: CreateProAccountTeamNameViewModel

    @MockK
    private lateinit var getTeamNameUseCase: GetTeamNameUseCase

    @MockK
    private lateinit var updateTeamNameUseCase: UpdateTeamNameUseCase

    @Before
    fun setup() {
        coEvery { getTeamNameUseCase.run(Unit) } returns Either.Right(TEST_TEAM_NAME)
        viewModel = CreateProAccountTeamNameViewModel(coroutinesTestRule.dispatcherProvider, getTeamNameUseCase, updateTeamNameUseCase)
    }

    @Test
    fun `given viewModel is initialised, when teamName is available, propagate teamName up to the view`() {
        coroutinesTestRule.runTest {
            viewModel.teamNameLiveData.awaitValue() shouldBeEqualTo TEST_TEAM_NAME
        }
    }

    @Test
    fun `given empty team name, when on team name text is changed, confirmation button should be disabled`() {
        coroutinesTestRule.runTest {
            viewModel.onTeamNameTextChanged(String.EMPTY)

            viewModel.confirmationButtonEnabled.awaitValue() shouldBe false
        }
    }

    @Test
    fun `given empty team name, when on team name text is changed, confirmation button should be enabled`() {
        coroutinesTestRule.runTest {
            viewModel.onTeamNameTextChanged(TEST_TEAM_NAME)

            viewModel.confirmationButtonEnabled.awaitValue() shouldBe true
        }
    }

    companion object {
        private const val CONFIG_URL = "https://wire.com"
        private const val TEAM_ABOUT_URL_SUFFIX = "/products/pro-secure-team-collaboration/"
        private const val TEST_TEAM_NAME = "teamName"
    }
}
