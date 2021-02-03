package com.wire.android.shared.team.usecase

import com.wire.android.UnitTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore("Class implementation is not complete")
class GetUserTeamUseCaseTest : UnitTest() {

    private lateinit var getUserTeamUseCase: GetUserTeamUseCase

    @Before
    fun setUp() {
        getUserTeamUseCase = GetUserTeamUseCase()
    }

    @Test
    fun `given run is called with a user, when user belongs to a team, then propagates team as success`() {

    }

    @Test
    fun `given run is called with a user, when user does not belong to a team, then propagates NotATeamUser failure`() {

    }

    @Test
    fun `given run is called with a user, when user team check fails, then propagates the failure`() {

    }
}
