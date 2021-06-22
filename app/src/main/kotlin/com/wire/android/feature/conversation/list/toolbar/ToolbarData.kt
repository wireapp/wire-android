package com.wire.android.feature.conversation.list.toolbar

import com.wire.android.shared.team.Team
import com.wire.android.shared.user.User

data class ToolbarData(
    val user: User,
    val team: Team?
)
