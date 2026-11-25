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
package com.wire.android.feature.cells.ui.edit

import android.content.Context
import android.content.Intent
import com.wire.android.feature.cells.ui.CellFeatureFlags
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import dev.zacsweers.metro.Inject

/** Opens the online document editor in an in-app screen, never in a browser. */
class OnlineEditor @Inject constructor(
    private val context: Context,
    private val isWireCellsEnabled: IsWireCellsEnabledUseCase,
    private val featureFlags: CellFeatureFlags,
) {

    /** Opens read-only when asked to, and always once the team's Drive is offboarded. */
    suspend fun open(url: String, readOnly: Boolean = false) {
        val isReadOnly = readOnly || (featureFlags.driveOffboardingEnabled && !isWireCellsEnabled())
        val editorUrl = if (isReadOnly) url.asReadOnlyEditorUrl() else url
        context.startActivity(OnlineEditorActivity.newIntent(context, editorUrl).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    internal companion object {
        private val PERMISSION_PARAM = Regex("([?&])permission=[^&#]*")

        fun String.asReadOnlyEditorUrl(): String =
            if (PERMISSION_PARAM.containsMatchIn(this)) {
                replace(PERMISSION_PARAM, "$1permission=read")
            } else {
                this + (if ('?' in this) "&" else "?") + "permission=read"
            }
    }
}
