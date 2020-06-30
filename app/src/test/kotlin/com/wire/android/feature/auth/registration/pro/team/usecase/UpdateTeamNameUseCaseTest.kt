package com.wire.android.feature.auth.registration.pro.team.usecase

import com.wire.android.UnitTest
import com.wire.android.eq
import com.wire.android.feature.auth.registration.pro.team.data.TeamsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
class UpdateTeamNameUseCaseTest : UnitTest() {

    private lateinit var updateTeamNameUseCase: UpdateTeamNameUseCase

    @Mock
    private lateinit var teamsRepository: TeamsRepository

    @Before
    fun setup() {
        updateTeamNameUseCase = UpdateTeamNameUseCase(teamsRepository)
    }

    @Test
    fun `given team name, then request update to repository`() {
        runBlockingTest {
            val params = UpdateTeamNameParams(TEST_TEAM_NAME)

            updateTeamNameUseCase.run(params)

            verify(teamsRepository).updateTeamName(eq(TEST_TEAM_NAME))

        }
    }

    companion object {
        private const val TEST_TEAM_NAME = "teamName"
    }
}
