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
package com.wire.android.ui.calling.ongoing.toast

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.stringWithStyledArgs

sealed class InCallToast(open val time: Long) {
    abstract val id: String

    data class Fullscreen(
        override val time: Long,
        val type: Type
    ) : InCallToast(time) {
        override val id: String = type.id

        enum class Type(val id: String) {
            DoubleTapToOpen("fullscreen_toggle_double_tap_to_open"),
            DoubleTapToClose("fullscreen_toggle_double_tap_to_close"),
        }
    }

    data class ModerationAction(
        override val time: Long,
        val actionId: String,
        val moderatorName: String,
        val type: Type,
    ) : InCallToast(time) {
        override val id: String = type.id

        enum class Type(val id: String) {
            Muted("moderation_action_muted")
        }
    }
}

@Composable
fun InCallToast(
    toast: InCallToast,
    onClick: (toastId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = when (toast) {
            is InCallToast.Fullscreen -> when (toast.type) {
                InCallToast.Fullscreen.Type.DoubleTapToOpen -> styledText(R.string.calling_ongoing_double_tap_for_full_screen)
                InCallToast.Fullscreen.Type.DoubleTapToClose -> styledText(R.string.calling_ongoing_double_tap_to_go_back)
            }

            is InCallToast.ModerationAction -> when (toast.type) {
                InCallToast.ModerationAction.Type.Muted -> styledText(R.string.calling_ongoing_muted_by, toast.moderatorName)
            }
        },
        modifier = modifier
            .background(color = colorsScheme().primary, shape = RoundedCornerShape(dimensions().corner12x))
            .clip(RoundedCornerShape(dimensions().corner12x))
            .clickable {
                onClick(toast.id)
            }
            .padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x),
    )
}

@Composable
private fun styledText(@StringRes stringResId: Int, vararg formatArgs: String) = LocalContext.current.resources.stringWithStyledArgs(
    stringResId = stringResId,
    normalStyle = typography().body01,
    normalColor = colorsScheme().onPrimary,
    argsStyle = typography().body02,
    argsColor = colorsScheme().onPrimary,
    formatArgs = formatArgs
)

@PreviewMultipleThemes
@Composable
fun InCallToastPreview() = WireTheme {
    InCallToast(
        toast = InCallToast.ModerationAction(0L, "1", "Alice", InCallToast.ModerationAction.Type.Muted),
        onClick = {},
    )
}
