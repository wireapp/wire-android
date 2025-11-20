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

data class Metrics(
    val success: Boolean,
    val estab_time_ms: Long,
    val setup_time_ms: Long,
    val avg_rate_u: Long,
    val avg_rate_d: Long
) {
    override fun toString(): String {
        return "Metrics(success=$success, estab_time_ms=$estab_time_ms, " +
                "setup_time_ms=$setup_time_ms, avg_rate_u=$avg_rate_u, avg_rate_d=$avg_rate_d)"
    }
}
