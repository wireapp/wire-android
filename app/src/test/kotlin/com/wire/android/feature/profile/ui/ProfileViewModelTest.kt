package com.wire.android.feature.profile.ui

import com.wire.android.UnitTest
import com.wire.android.core.functional.Either
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.shouldBeUpdated
import com.wire.android.framework.livedata.shouldNotBeUpdated
import com.wire.android.shared.team.Team
import com.wire.android.shared.team.usecase.GetUserTeamUseCase
import com.wire.android.shared.team.usecase.GetUserTeamUseCaseParams
import com.wire.android.shared.team.usecase.NotATeamUser
import com.wire.android.shared.user.User
import com.wire.android.shared.user.usecase.GetCurrentUserUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ProfileViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @MockK
    private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    @MockK
    private lateinit var getUserTeamUseCase: GetUserTeamUseCase

    private lateinit var profileViewModel: ProfileViewModel

    @Before
    fun setUp() {
        profileViewModel = ProfileViewModel(
            coroutinesTestRule.dispatcherProvider,
            getCurrentUserUseCase, getUserTeamUseCase
        )
    }

    @Test
    fun `given fetchProfileInfo is called, when getCurrentUserUseCase fails, then sets error and does not update user info`() {
        coEvery { getCurrentUserUseCase.run(any()) } returns Either.Left(mockk())

        profileViewModel.fetchProfileInfo()

        profileViewModel.errorLiveData shouldBeUpdated {}
        profileViewModel.currentUserLiveData.shouldNotBeUpdated()
        coVerify(exactly = 1) { getCurrentUserUseCase.run(any()) }
    }

    @Test
    fun `given fetchProfileInfo is called, when getCurrentUserUseCase returns a user, then updates user info`() {
        val user = mockk<User>(relaxed = true)
        coEvery { getCurrentUserUseCase.run(any()) } returns Either.Right(user)
        coEvery { getUserTeamUseCase.run(any()) } returns Either.Left(mockk())

        profileViewModel.fetchProfileInfo()

        profileViewModel.currentUserLiveData shouldBeUpdated { it shouldBeEqualTo user }
        coVerify(exactly = 1) { getCurrentUserUseCase.run(any()) }
    }


    @Test
    fun `given fetchProfileInfo called & curr user is fetched, when getUserTeamUC fails w NotATeamUser, then propagates null team name`() {
        val user = mockk<User>()
        coEvery { getCurrentUserUseCase.run(any()) } returns Either.Right(user)
        coEvery { getUserTeamUseCase.run(any()) } returns Either.Left(NotATeamUser)

        profileViewModel.fetchProfileInfo()

        profileViewModel.teamNameLiveData shouldBeUpdated { it shouldBeEqualTo null }

        val useCaseParamsSlot = slot<GetUserTeamUseCaseParams>()
        coVerify(exactly = 1) { getUserTeamUseCase.run(capture(useCaseParamsSlot)) }
        useCaseParamsSlot.captured.user shouldBeEqualTo user
    }

    @Test
    fun `given fetchProfileInfo called & current user is fetched, when getUserTeamUseCase fails with other error, then sets error`() {
        val user = mockk<User>()
        coEvery { getCurrentUserUseCase.run(any()) } returns Either.Right(user)
        coEvery { getUserTeamUseCase.run(any()) } returns Either.Left(mockk())

        profileViewModel.fetchProfileInfo()

        profileViewModel.errorLiveData shouldBeUpdated {}
        profileViewModel.teamNameLiveData.shouldNotBeUpdated()
        coVerify(exactly = 1) { getUserTeamUseCase.run(any()) }
    }

    @Test
    fun `given fetchProfileInfo called & current user is fetched, when getUserTeamUseCase returns a team, then propagates team name`() {
        val user = mockk<User>()
        coEvery { getCurrentUserUseCase.run(any()) } returns Either.Right(user)

        val team = mockk<Team>()
        every { team.name } returns TEST_TEAM_NAME
        coEvery { getUserTeamUseCase.run(any()) } returns Either.Right(team)

        profileViewModel.fetchProfileInfo()

        profileViewModel.teamNameLiveData shouldBeUpdated { it shouldBeEqualTo TEST_TEAM_NAME }
        coVerify(exactly = 1) { getUserTeamUseCase.run(any()) }
    }

    companion object {
        private const val TEST_TEAM_NAME = "Wire Team"
    }
}