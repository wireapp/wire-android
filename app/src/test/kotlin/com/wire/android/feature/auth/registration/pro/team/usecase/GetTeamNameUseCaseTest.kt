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
class GetTeamNameUseCaseTest : UnitTest() {

    private lateinit var getTeamNameUseCase: GetTeamNameUseCase

    @MockK
    private lateinit var teamsRepository: TeamsRepository

    @Before
    fun setup() {
        getTeamNameUseCase = GetTeamNameUseCase(teamsRepository)
    }

    @Test
    fun `given teamName is request, then request repository for team name`() {
        runBlocking { getTeamNameUseCase.run(Unit) }

        coVerify(exactly = 1) { teamsRepository.teamName() }
    }
}
