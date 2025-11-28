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

data class Instance(
    val id: String?,
    val instanceStatus: InstanceStatus?,
    val currentCall: Call?,
    val email: String?,
    val password: String?,
    val backend: String?,
    val screenshot: String?,
    val instanceType: VersionedInstanceType?,
    val name: String?,
    val timeout: Long?,
    val created: Long?,
    val message: String? = null
) {
    override fun toString(): String {
        if (id == null && instanceStatus == null && message != null) return message
        return "Instance(id=$id, instanceStatus=$instanceStatus, email=$email, password=$password, " +
                "currentCall=$currentCall, backend=$backend, instanceType=$instanceType, " +
                "screenshot=<suppressed>, name=$name, timeout=$timeout, created=$created)"
    }
}
