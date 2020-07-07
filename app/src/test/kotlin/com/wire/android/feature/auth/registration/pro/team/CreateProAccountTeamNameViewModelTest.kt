package com.wire.android.feature.auth.registration.pro.team

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.wire.android.UnitTest
import com.wire.android.core.accessibility.Accessibility
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.registration.pro.team.usecase.GetTeamNameUseCase
import com.wire.android.feature.auth.registration.pro.team.usecase.UpdateTeamNameUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.awaitValue
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

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

    @Mock
    private lateinit var accessibility: Accessibility

    @Before
    fun setup() {
        runBlocking { `when`(getTeamNameUseCase.run(Unit)).thenReturn(Either.Right(TEST_TEAM_NAME)) }
        viewModel = CreateProAccountTeamNameViewModel(
            getTeamNameUseCase,
            updateTeamNameUseCase,
            accessibility
        )
    }

    @Test
    fun `given shouldFocusInput is queried, when talkback is not enabled, then return true`() {
        runBlockingTest {
            `when`(accessibility.isTalkbackEnabled()).thenReturn(false)
            assertThat(viewModel.shouldFocusInput()).isEqualTo(true)
        }
    }

    @Test
    fun `given shouldFocusInput is queried, when talkback is enabled, then return false `() {
        runBlockingTest {
            `when`(accessibility.isTalkbackEnabled()).thenReturn(true)
            assertThat(viewModel.shouldFocusInput()).isEqualTo(false)
        }
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
