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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.ramcosta.composedestinations.spec.Direction
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.AndroidReleaseNotesDestination
import com.wire.android.navigation.ExternalUriDirection
import com.wire.android.navigation.WelcomeToNewAndroidAppDestination
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.shimmerPlaceholder
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText

@Composable
fun WhatsNewItem(
    modifier: Modifier = Modifier,
    title: String? = null,
    boldTitle: Boolean = false,
    text: String? = null,
    contentDescription: String = "${title ?: ""} ${text ?: ""}",
    @DrawableRes trailingIcon: Int? = null,
    onRowPressed: Clickable = Clickable(false),
    isLoading: Boolean = false,
) {
    RowItemTemplate(
        title = {
            if (!title.isNullOrBlank()) {
                Text(
                    style = if (boldTitle) MaterialTheme.wireTypography.body02 else MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    text = title,
                    modifier = Modifier
                        .padding(start = dimensions().spacing8x)
                        .shimmerPlaceholder(visible = isLoading)
                )
            }
        },
        subtitle = {
            if (!text.isNullOrBlank()) {
                Text(
                    style = MaterialTheme.wireTypography.label04,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    text = text,
                    modifier = Modifier
                        .padding(start = dimensions().spacing8x, top = dimensions().spacing8x)
                        .shimmerPlaceholder(visible = isLoading)
                )
            }
        },
        actions = {
            trailingIcon?.let {
                Icon(
                    painter = painterResource(id = trailingIcon),
                    contentDescription = null,
                    tint = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
                    modifier = Modifier
                        .defaultMinSize(dimensions().wireIconButtonSize)
                        .padding(end = dimensions().spacing8x)
                        .shimmerPlaceholder(visible = isLoading)
                )
            } ?: Icons.Filled.ChevronRight
        },
        clickable = onRowPressed,
        modifier = modifier
            .padding(vertical = dimensions().spacing4x)
            .clearAndSetSemantics { this.contentDescription = contentDescription }
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

    data class AllAndroidReleaseNotes(
        override val id: String = "android_release_notes"
    ) : WhatsNewItem(
        id = id,
        title = UIText.StringResource(R.string.whats_new_android_release_notes_label),
        direction = AndroidReleaseNotesDestination
    )

    data class AndroidReleaseNotes(
        override val id: String,
        override val title: UIText,
        override val boldTitle: Boolean = false,
        override val text: UIText? = null,
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
fun PreviewFileRestrictionDialog() {
    WireTheme {
        WhatsNewItem(
            title = "What's new item",
            text = "This is the text of the item",
            trailingIcon = R.drawable.ic_arrow_right,
            isLoading = false,
            onRowPressed = Clickable(enabled = true) {}
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewFileRestrictionDialogLoading() {
    WireTheme {
        WhatsNewItem(
            title = "What's new item",
            text = "This is the text of the item",
            trailingIcon = R.drawable.ic_arrow_right,
            isLoading = true,
            onRowPressed = Clickable(enabled = false) {}
        )
    }
}
