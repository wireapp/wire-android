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
package com.wire.android.util.deeplink

import android.net.Uri
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper

object UserLinkQRMapper {

    val qualifiedIdMapper = QualifiedIdMapper(null)

    fun fromDeepLinkToQualifiedId(uri: Uri, defaultDomain: String): UserLinkQRResult {
        val segments = uri.pathSegments
        return when (segments.size) {
            1 -> {
                val userId = qualifiedIdMapper.fromStringToQualifiedID(segments.last())
                val sanitizedId = userId.value.toDefaultQualifiedId(
                    userDomain = userId.domain.takeIf {
                        it.isNotBlank()
                    } ?: defaultDomain
                )
                UserLinkQRResult.Success(sanitizedId)
            }

            2 -> {
                val domain = segments.first()
                val userId = segments.last()
                UserLinkQRResult.Success(userId.toDefaultQualifiedId(domain))
            }

            else -> {
                UserLinkQRResult.Failure
            }
        }
    }

    /**
     * Converts the string to a [QualifiedID] with the current user domain or default.
     * IMPORTANT! This also handles the special case where iOS is sending the ID in uppercase.
     */
    private fun String.toDefaultQualifiedId(userDomain: String): QualifiedID {
        return QualifiedID(this.lowercase(), userDomain)
    }

    sealed class UserLinkQRResult {
        data class Success(val qualifiedUserId: QualifiedID) : UserLinkQRResult()
        data object Failure : UserLinkQRResult()
    }
}
