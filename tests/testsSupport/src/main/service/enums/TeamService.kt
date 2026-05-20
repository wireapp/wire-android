/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package service.enums

enum class TeamService(
    val serviceName: String,
    val providerId: String,
    val serviceId: String
) {
    ECHO(
        "Echo",
        "d64af9ae-e0c5-4ce6-b38a-02fd9363b54c",
        "d693bd64-79ae-4970-ad12-4df49cfe4038"
    ),
    POLL_BOT(
        "Poll Bot",
        "d1e52fa0-46bc-46fa-acc1-95bd91735de1",
        "40085205-4499-4cd7-a093-ca7d3c1d8b21"
    ),
    TRACKER(
        "Tracker",
        "d64af9ae-e0c5-4ce6-b38a-02fd9363b54c",
        "7ba4aac9-1bbb-41bd-b782-b57157665157"
    );

    companion object {
        fun fromName(serviceName: String): TeamService {
            return entries.firstOrNull { it.serviceName == serviceName }
                ?: throw IllegalArgumentException("No such service: $serviceName")
        }
    }
}
