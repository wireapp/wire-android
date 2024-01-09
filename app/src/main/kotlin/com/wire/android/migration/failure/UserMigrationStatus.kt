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
package com.wire.android.migration.failure

@Suppress("MagicNumber")
sealed interface UserMigrationStatus {
    val value: Int

    /**
     * No need to migrate the user.
     * user had no local databse to migrate from when updating from scala
     */
    object NoNeed : UserMigrationStatus {
        override val value: Int = 0
    }

    /**
     * User migration has not started yet.
     * it can be that the user is not logged in or the migration has not started yet
     */
    object NotStarted : UserMigrationStatus {
        override val value: Int = 1
    }

    /**
     * User migration has completed.
     */
    @Deprecated("Use CompletedWithErrors, or Successfully instead")
    object Completed : UserMigrationStatus {
        override val value: Int = 2
    }

    object CompletedWithErrors : UserMigrationStatus {
        override val value: Int = 3
    }

    object Successfully : UserMigrationStatus {
        override val value: Int = 4
    }

    companion object {
        fun fromInt(value: Int): UserMigrationStatus = when (value) {
            0 -> NoNeed
            1 -> NotStarted
            2 -> Completed
            3 -> CompletedWithErrors
            4 -> Successfully
            else -> throw IllegalArgumentException("Invalid value for UserMigrationStatus: $value")
        }
    }
}
