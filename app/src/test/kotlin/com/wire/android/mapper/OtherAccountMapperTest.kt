/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.mapper

import com.wire.android.ui.home.conversations.avatar
import com.wire.android.ui.userprofile.self.model.OtherAccount
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.SelfUser
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OtherAccountMapperTest {

    @Test
    fun givenSelfUsersAndTeams_whenMappingToOtherAccounts_thenCorrectValuesShouldBeReturned() = runTest {
        val (arrangement, mapper) = Arrangement().arrange()
        // Given
        val data: List<Pair<SelfUser, Team?>> = listOf(
            testSelfUser(0) to Team("0", "team_name", "icon"),
            testSelfUser(1) to null
        )
        // When
        val results = data.map { (selfUser, team) -> mapper.toOtherAccount(selfUser, team) }
        // Then
        results.forEachIndexed { index, result ->
            val (selfUser, team) = data[index]
            assert(compareResult(arrangement.wireSessionImageLoader, selfUser, team, result))
        }
    }

    private fun compareResult(
        wireSessionImageLoader: WireSessionImageLoader,
        selfUser: SelfUser,
        team: Team?,
        otherAccount: OtherAccount
    ): Boolean =
        selfUser.id == otherAccount.id
            && selfUser.name == otherAccount.fullName
            && selfUser.avatar(wireSessionImageLoader, selfUser.connectionStatus) == otherAccount.avatarData
            && team?.name == otherAccount.teamName

    private class Arrangement {

        @MockK
        lateinit var wireSessionImageLoader: WireSessionImageLoader

        private val mapper: OtherAccountMapper by lazy { OtherAccountMapper(wireSessionImageLoader) }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun arrange() = this to mapper
    }
}
