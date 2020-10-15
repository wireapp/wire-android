package com.wire.android.feature.auth.registration.pro.team

import com.wire.android.UnitTest
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.registration.pro.team.usecase.GetTeamNameUseCase
import com.wire.android.feature.auth.registration.pro.team.usecase.UpdateTeamNameUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.awaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

@ExperimentalCoroutinesApi
class CreateProAccountTeamNameViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    private lateinit var viewModel: CreateProAccountTeamNameViewModel

    @Mock
    private lateinit var getTeamNameUseCase: GetTeamNameUseCase

    @Mock
    private lateinit var updateTeamNameUseCase: UpdateTeamNameUseCase

    @Before
    fun setup() {
        runBlocking { `when`(getTeamNameUseCase.run(Unit)).thenReturn(Either.Right(TEST_TEAM_NAME)) }
        viewModel = CreateProAccountTeamNameViewModel(coroutinesTestRule.dispatcherProvider, getTeamNameUseCase, updateTeamNameUseCase)
    }

    @Test
    fun `given viewModel is initialised, when teamName is available, propagate teamName up to the view`() {
        coroutinesTestRule.runTest {
            assertThat(viewModel.teamNameLiveData.awaitValue()).isEqualTo(TEST_TEAM_NAME)
        }
    }

    @Test
    fun `given empty team name, when on team name text is changed, confirmation button should be disabled`() {
        coroutinesTestRule.runTest {
            viewModel.onTeamNameTextChanged(String.EMPTY)
            assertThat(viewModel.confirmationButtonEnabled.awaitValue()).isFalse()
        }
    }

    @Test
    fun `given empty team name, when on team name text is changed, confirmation button should be enabled`() {
        coroutinesTestRule.runTest {
            viewModel.onTeamNameTextChanged(TEST_TEAM_NAME)
            assertThat(viewModel.confirmationButtonEnabled.awaitValue()).isTrue()
        }
    }

    companion object {
        private const val CONFIG_URL = "https://wire.com"
        private const val TEAM_ABOUT_URL_SUFFIX = "/products/pro-secure-team-collaboration/"
        private const val TEST_TEAM_NAME = "teamName"
    }
}
