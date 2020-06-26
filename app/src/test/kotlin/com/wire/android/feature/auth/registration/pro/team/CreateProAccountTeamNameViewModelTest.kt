package com.wire.android.feature.auth.registration.pro.team

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.wire.android.UnitTest
import com.wire.android.capture
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.registration.pro.team.usecase.GetTeamNameUseCase
import com.wire.android.feature.auth.registration.pro.team.usecase.UpdateTeamNameParams
import com.wire.android.feature.auth.registration.pro.team.usecase.UpdateTeamNameUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.awaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
class CreateProAccountTeamNameViewModelTest : UnitTest() {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    private lateinit var viewModel: CreateProAccountTeamNameViewModel

    @Mock
    private lateinit var getTeamNameUseCase: GetTeamNameUseCase

    @Mock
    private lateinit var updateTeamNameUseCase: UpdateTeamNameUseCase

    @Captor
    private lateinit var paramsArgumentCaptor: ArgumentCaptor<UpdateTeamNameParams>

    @Before
    fun setup() {
        runBlocking { `when`(getTeamNameUseCase.run(Unit)).thenReturn(Either.Right(TEST_TEAM_NAME)) }
        viewModel = CreateProAccountTeamNameViewModel(getTeamNameUseCase, updateTeamNameUseCase)
    }

    @Test
    fun `given viewModel is initialised, when teamName is available, propagate teamName up to the view`() {
        runBlockingTest {
            assertEquals(TEST_TEAM_NAME, viewModel.teamNameLiveData.awaitValue())
        }
    }

    @Test
    fun `given about button is clicked, when url is provided, propagate url back to the view`() {
        runBlockingTest {
            viewModel.onAboutButtonClicked()
            assertEquals("$CONFIG_URL$TEAM_ABOUT_URL_SUFFIX", viewModel.urlLiveData.awaitValue())
        }
    }

    @Test
    fun `given empty team name, when on team name text is changed, confirmation button should be disabled`() {
        runBlockingTest {
            viewModel.onTeamNameTextChanged(String.EMPTY)
            assertFalse(viewModel.confirmationButtonEnabled.awaitValue())
        }
    }

    @Test
    fun `given empty team name, when on team name text is changed, confirmation button should be enabled`() {
        runBlockingTest {
            viewModel.onTeamNameTextChanged(TEST_TEAM_NAME)
            assertTrue(viewModel.confirmationButtonEnabled.awaitValue())
        }
    }

    companion object {
        private const val CONFIG_URL = "https://wire.com"
        private const val TEAM_ABOUT_URL_SUFFIX = "/products/pro-secure-team-collaboration/"
        private const val TEST_TEAM_NAME = "teamName"
    }
}
