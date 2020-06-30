package com.wire.android.feature.auth.registration.pro.team.usecase

import com.wire.android.UnitTest
import com.wire.android.feature.auth.registration.pro.team.data.TeamsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
class GetTeamNameUseCaseTest : UnitTest() {

    private lateinit var getTeamNameUseCase: GetTeamNameUseCase

    @Mock
    private lateinit var teamsRepository: TeamsRepository

    @Before
    fun setup() {
        getTeamNameUseCase = GetTeamNameUseCase(teamsRepository)
    }

    @Test
    fun `given teamName is request, then request repository for team name`() {
        runBlockingTest {
            getTeamNameUseCase.run(Unit)

            verify(teamsRepository).teamName()
        }
    }
}
