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
package com.wire.android.feature.cells.ui

import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.conversation.IsSelfUserViewerOnConversationUseCase
import dev.zacsweers.metro.Inject

/** Tells whether the self user only has viewer access to the files a screen shows. */
class CellViewerAccess @Inject constructor(
    private val isSelfUserViewerOnConversation: IsSelfUserViewerOnConversationUseCase,
    private val qualifiedIdMapper: QualifiedIdMapper,
    private val featureFlags: CellFeatureFlags,
) {

    /** Only the files of a conversation have access restrictions, and only with drive permissions enabled. */
    suspend fun isViewerOnly(context: CellScreenContext): Boolean {
        if (!featureFlags.drivePermissionsEnabled) return false
        val conversationId = context.rootConversationId?.takeIf { context.isConversationFiles } ?: return false
        return !isSelfUserViewerOnConversation(qualifiedIdMapper.fromStringToQualifiedID(conversationId))
    }
}
