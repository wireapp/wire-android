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
package com.wire.android.ui.common.preview

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import de.drick.compose.edgetoedgepreviewlib.CameraCutoutMode
import de.drick.compose.edgetoedgepreviewlib.EdgeToEdgeTemplate
import de.drick.compose.edgetoedgepreviewlib.NavigationMode

@Composable
fun EdgeToEdgePreview(
    useDarkIcons: Boolean,
    content: @Composable () -> Unit,
) {
    // isAppearanceLightStatusBars cannot be set for previews, so we need to apply it manually
    // TODO: remove LocalConfiguration and just pass `isDarkMode` into EdgeToEdgeTemplate after updating edgetoedgepreviewlib to 0.6.0
    CompositionLocalProvider(
        LocalConfiguration provides LocalConfiguration.current.apply {
            uiMode = if (useDarkIcons) UI_MODE_NIGHT_NO else UI_MODE_NIGHT_YES
        }
    ) {
        EdgeToEdgeTemplate(
            navMode = NavigationMode.Gesture,
            cameraCutoutMode = CameraCutoutMode.Middle,
            isInvertedOrientation = false,
            showInsetsBorder = false,
            content = content,
        )
    }
}
