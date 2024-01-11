/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.home.whatsnew

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ramcosta.composedestinations.spec.Direction
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.AndroidReleaseNotesDestination
import com.wire.android.navigation.ExternalUriDirection
import com.wire.android.navigation.WelcomeToNewAndroidAppDestination
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText

@Composable
fun WhatsNewItem(
    title: String? = null,
    boldTitle: Boolean = false,
    text: String? = null,
    @DrawableRes trailingIcon: Int? = null,
    onRowPressed: Clickable = Clickable(false),
) {
    RowItemTemplate(
        title = {
            if (!title.isNullOrBlank()) {
                Text(
                    style = if (boldTitle) MaterialTheme.wireTypography.body02 else MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    text = title,
                    modifier = Modifier.padding(start = dimensions().spacing8x)
                )
            }
        },
        subtitle = {
            if (!text.isNullOrBlank()) {
                Text(
                    style = MaterialTheme.wireTypography.label04,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    text = text,
                    modifier = Modifier.padding(start = dimensions().spacing8x, top = dimensions().spacing8x)
                )
            }
        },
        actions = {
            trailingIcon?.let {
                Icon(
                    painter = painterResource(id = trailingIcon),
                    contentDescription = "",
                    tint = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
                    modifier = Modifier
                        .defaultMinSize(dimensions().wireIconButtonSize)
                        .padding(end = dimensions().spacing8x)
                )
            } ?: Icons.Filled.ChevronRight
        },
        clickable = onRowPressed,
        modifier = Modifier.padding(vertical = dimensions().spacing4x)
    )
}

sealed class WhatsNewItem(
    val direction: Direction,
    open val id: String,
    open val title: UIText,
    open val boldTitle: Boolean = false,
    open val text: UIText? = null,
) {
    data object WelcomeToNewAndroidApp : WhatsNewItem(
        id = "welcome_to_new_android_app",
        title = UIText.StringResource(R.string.whats_new_welcome_to_new_android_app_label),
        direction = WelcomeToNewAndroidAppDestination
    )

    data object AllAndroidReleaseNotes : WhatsNewItem(
        id = "android_release_notes",
        title = UIText.StringResource(R.string.whats_new_android_release_notes_label),
        direction = AndroidReleaseNotesDestination
    )

    data class AndroidReleaseNotes(
        override val id: String,
        override val title: UIText,
        override val boldTitle: Boolean,
        override val text: UIText?,
        val url: String
    ) : WhatsNewItem(
        id = id,
        title = title,
        boldTitle = boldTitle,
        text = text,
        direction = object : ExternalUriDirection {
            override val uri: Uri
                get() = Uri.parse(url)
        }
    )
}

@PreviewMultipleThemes
@Composable
fun previewFileRestrictionDialog() {
    WireTheme {
        WhatsNewItem(
            title = "What's new item",
            text = "This is the text of the item",
            trailingIcon = R.drawable.ic_arrow_right
        )
    }
}
