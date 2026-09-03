/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.userprofile.teammigration

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

/**
 * One personal-to-team migration owns one [TeamMigrationViewModel] across all four steps.
 *
 * The explicit flow id is serialized with every entry, so Navigation 3 can restore the shared
 * ViewModel owner without deriving ownership from a mutable back-stack position.
 */
sealed interface TeamMigrationRoute : SessionRoute {
    override val flowId: String
}

@Serializable
data class TeamMigrationTeamPlanRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    val isMigrationDotActive: Boolean = false,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : TeamMigrationRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        validateTeamMigrationFlowId(flowId)
    }

    companion object {
        const val ROUTE_ID = "app/team_migration_team_plan_step_screen"

        fun start(
            sessionId: WireSessionId,
            isMigrationDotActive: Boolean = false,
            entryId: WireNavEntryId = WireNavEntryId.random(),
        ) = TeamMigrationTeamPlanRoute(
            sessionId = sessionId,
            flowId = "team-migration:${entryId.value}",
            isMigrationDotActive = isMigrationDotActive,
            entryId = entryId,
        )
    }
}

@Serializable
data class TeamMigrationTeamNameRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : TeamMigrationRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        validateTeamMigrationFlowId(flowId)
    }

    companion object {
        const val ROUTE_ID = "app/team_migration_team_name_step_screen"
    }
}

@Serializable
data class TeamMigrationConfirmationRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : TeamMigrationRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        validateTeamMigrationFlowId(flowId)
    }

    companion object {
        const val ROUTE_ID = "app/team_migration_confirmation_step_screen"
    }
}

@Serializable
data class TeamMigrationDoneRoute(
    override val sessionId: WireSessionId,
    override val flowId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : TeamMigrationRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        validateTeamMigrationFlowId(flowId)
    }

    companion object {
        const val ROUTE_ID = "app/team_migration_done_step_screen"
    }
}

private fun validateTeamMigrationFlowId(flowId: String) {
    require(flowId.isNotBlank()) { "A team-migration flow id cannot be blank" }
}
