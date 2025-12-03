/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

package call.models

enum class InstanceStatus {
    NON_EXISTENT,
    STARTING,
    STARTED,
    STOPPING,
    STOPPED,
    LOGIN_FAILED,
    DESTROYED,
    ERROR;

    companion object {
        /**
         * Checks if the given [item] is contained within the provided [subset].
         */
        fun isContainedInSubset(subset: Array<InstanceStatus>, item: InstanceStatus): Boolean {
            return subset.contains(item)
        }

        /**
         * Converts a string to an [InstanceStatus] enum value.
         * Case-insensitive. Throws [IllegalArgumentException] if the string is unknown.
         */
        fun fromString(value: String): InstanceStatus {
            return entries
                .firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Instance status '$value' is unknown")
        }
    }
}
