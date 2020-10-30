package com.wire.android.feature.auth.registration.pro.team.usecase

import com.wire.android.UnitTest
import com.wire.android.feature.auth.registration.pro.team.data.TeamsRepository
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class UpdateTeamNameUseCaseTest : UnitTest() {

    private lateinit var updateTeamNameUseCase: UpdateTeamNameUseCase

    @MockK
    private lateinit var teamsRepository: TeamsRepository

    @Before
    fun setup() {
        updateTeamNameUseCase = UpdateTeamNameUseCase(teamsRepository)
    }

    @Test
    fun `given team name, then request update to repository`() {
        val params = UpdateTeamNameParams(TEST_TEAM_NAME)

        runBlocking { updateTeamNameUseCase.run(params) }

        coVerify(exactly = 1) { teamsRepository.updateTeamName(TEST_TEAM_NAME) }
    }

    companion object {
        private const val TEST_TEAM_NAME = "teamName"
    }
}
