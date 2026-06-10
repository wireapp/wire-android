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

import kotlinx.serialization.Serializable

@Serializable
data class Instance(
    val id: String? = null,
    val instanceStatus: InstanceStatus? = null,
    val currentCall: Call? = null,
    val email: String? = null,
    val password: String? = null,
    val backend: String? = null,
    val screenshot: String? = null,
    val instanceType: VersionedInstanceType? = null,
    val name: String? = null,
    val timeout: Long? = null,
    val created: Long? = null,
    val message: String? = null
) {
    override fun toString(): String {
        if (id == null && instanceStatus == null && message != null) return message
        return "Instance(id=$id, instanceStatus=$instanceStatus, email=$email, password=$password, " +
                "currentCall=$currentCall, backend=$backend, instanceType=$instanceType, " +
                "screenshot=<suppressed>, name=$name, timeout=$timeout, created=$created)"
    }
}
