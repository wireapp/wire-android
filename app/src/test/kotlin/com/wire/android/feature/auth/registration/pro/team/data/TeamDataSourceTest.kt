package com.wire.android.feature.auth.registration.pro.team.data

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldSucceed
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class TeamDataSourceTest : UnitTest() {

    private lateinit var teamsRepository: TeamsRepository

    @Before
    fun setup() {
        teamsRepository = TeamDataSource()
    }

    @Test
    fun `given team name, when cache is updated, then check if name is correct`() {
        runBlocking {
            teamsRepository.updateTeamName(TEST_TEAM_NAME)

            teamsRepository.teamName() shouldSucceed { it shouldEqual TEST_TEAM_NAME }
        }
    }

    companion object {
        private const val TEST_TEAM_NAME = "teamName"
    }
}
