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
package service.models

import org.json.JSONArray
import user.utils.ClientUser
import java.io.File
import java.time.Duration

data class SendTextParams(
    val owner: ClientUser,
    val deviceName: String?,
    val convoDomain: String,
    val convoId: String,
    val timeout: Duration,
    val expectsReadConfirmation: Boolean,
    val text: String,
    val buttons: JSONArray,
    val legalHoldStatus: Int,
    val messageId: String? = null,
    val messageTimer: Duration = Duration.ZERO,
    val listOfMentions: List<Mentions> = emptyList()
)

data class SendTextWithLinkParams(
    val owner: ClientUser,
    val deviceName: String?,
    val convoDomain: String,
    val convoId: String,
    val timeout: Duration,
    val messageTimer: Duration = Duration.ZERO,
    val expectsReadConfirmation: Boolean,
    val text: String,
    val buttons: JSONArray,
    val legalHoldStatus: Int,
    val messageId: String,
    val summary: String,
    val imageFile: File,
    val title: String,
    val url: String,
    val urlOffset: String,
    val permUrl: String,
    val filePath: String,
    val imagePath: String? = null,
)

data class SendFileParams(
    val owner: ClientUser,
    val deviceName: String?,
    val convoDomain: String,
    val convoId: String,
    val timeout: Duration,
    val filePath: String,
    val type: String,
    val otherAlgorithm: Boolean,
    val otherHash: Boolean,
    val invalidHash: Boolean,
    val duration: Duration = Duration.ZERO,
    val normalizedLoudness: IntArray = intArrayOf(),
    val dimensions: IntArray = intArrayOf()
) {
    @Suppress("CyclomaticComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SendFileParams

        if (otherAlgorithm != other.otherAlgorithm) return false
        if (otherHash != other.otherHash) return false
        if (invalidHash != other.invalidHash) return false
        if (owner != other.owner) return false
        if (deviceName != other.deviceName) return false
        if (convoDomain != other.convoDomain) return false
        if (convoId != other.convoId) return false
        if (timeout != other.timeout) return false
        if (filePath != other.filePath) return false
        if (type != other.type) return false
        if (duration != other.duration) return false
        if (!normalizedLoudness.contentEquals(other.normalizedLoudness)) return false
        if (!dimensions.contentEquals(other.dimensions)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = otherAlgorithm.hashCode()
        result = 31 * result + otherHash.hashCode()
        result = 31 * result + invalidHash.hashCode()
        result = 31 * result + owner.hashCode()
        result = 31 * result + (deviceName?.hashCode() ?: 0)
        result = 31 * result + convoDomain.hashCode()
        result = 31 * result + convoId.hashCode()
        result = 31 * result + timeout.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + normalizedLoudness.contentHashCode()
        result = 31 * result + dimensions.contentHashCode()
        return result
    }
}

data class SendLocationParams(
    val owner: ClientUser,
    val deviceName: String?,
    val convoId: String,
    val convoDomain: String,
    val timeout: Duration,
    val longitude: Float,
    val latitude: Float,
    val locationName: String,
    val zoom: Int
)
