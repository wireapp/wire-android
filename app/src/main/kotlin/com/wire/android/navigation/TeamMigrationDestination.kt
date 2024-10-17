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
package com.wire.android.navigation

import com.ramcosta.composedestinations.spec.Direction
import com.wire.android.ui.destinations.TeamMigrationConfirmationStepScreenDestination
import com.wire.android.ui.destinations.TeamMigrationDoneStepScreenDestination

sealed class TeamMigrationDestination(
    val direction: Direction
) {

    data object Confirmation : TeamMigrationDestination(
        direction = TeamMigrationConfirmationStepScreenDestination
    )

    data object MigrationDone : TeamMigrationDestination(
        direction = TeamMigrationDoneStepScreenDestination
    )

    val itemName: String get() = ITEM_NAME_PREFIX + this

    companion object {
        private const val ITEM_NAME_PREFIX = "TeamMigrationNavigationItem."
        fun fromRoute(fullRoute: String): TeamMigrationDestination? =
            values().find { it.direction.route.getBaseRoute() == fullRoute.getBaseRoute() }

        fun values(): Array<TeamMigrationDestination> =
            arrayOf(Confirmation, MigrationDone)
    }
}
