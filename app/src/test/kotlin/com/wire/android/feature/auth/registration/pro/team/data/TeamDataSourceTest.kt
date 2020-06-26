package com.wire.android.feature.auth.registration.pro.team.data

import com.wire.android.UnitTest
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
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
        runBlockingTest {
            teamsRepository.updateTeamName(TEST_TEAM_NAME)

            teamsRepository.teamName().assertRight {
                assertEquals(it, TEST_TEAM_NAME)
            }
        }
    }

    companion object {
        private const val TEST_TEAM_NAME = "teamName"
    }
}
