package com.wire.android.framework

import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.team.Team

object TestTeam {
    val TEAM: Team = Team(id = "Some-team", name = "Some-name", icon = "icon")
    val TEAM_ID = TeamId("Some-team")
}
