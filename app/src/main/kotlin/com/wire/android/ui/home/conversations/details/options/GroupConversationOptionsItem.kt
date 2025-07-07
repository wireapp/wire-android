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

package com.wire.android.ui.home.conversations.details.options

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.WireRadioButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.clickable
import com.wire.android.ui.home.settings.SettingsOptionSwitch
import com.wire.android.ui.home.settings.SwitchState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun GroupConversationOptionsItem(
    title: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.wireColorScheme.surface)
        .defaultMinSize(minHeight = MaterialTheme.wireDimensions.conversationOptionsItemMinHeight),
    clickable: Clickable = Clickable(enabled = false, onClick = {}),
    arrowLabel: String? = null,
    arrowLabelColor: Color = MaterialTheme.wireColorScheme.secondaryText,
    subtitle: String? = null,
    label: String? = null,
    trailingOnText: String? = null,
    titleTrailingItem: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    switchState: SwitchState = SwitchState.None,
    titleStyle: TextStyle = MaterialTheme.wireTypography.body02,
    arrowType: ArrowType = ArrowType.CENTER_ALIGNED,
    contentDescription: String? = null,
    selected: Boolean? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(clickable)
            .semantics { contentDescription?.let { this.contentDescription = contentDescription } }
            .padding(
                top = MaterialTheme.wireDimensions.spacing12x,
                bottom = MaterialTheme.wireDimensions.spacing12x,
                start = MaterialTheme.wireDimensions.spacing16x,
                end = MaterialTheme.wireDimensions.spacing12x
            )
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.wireTypography.label01,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.spacing4x)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selected != null) {
                    WireRadioButton(
                        checked = selected,
                        modifier = Modifier.padding(end = MaterialTheme.wireDimensions.spacing8x),
                    )
                }
                Text(
                    text = title,
                    style = titleStyle,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    modifier = Modifier.weight(weight = 1f, fill = true)
                )
                if (titleTrailingItem != null) {
                    Box(modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing8x)) { titleTrailingItem() }
                }
                SettingsOptionSwitch(
                    switchState = switchState,
                    trailingOnText = trailingOnText
                )

                arrowLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.wireTypography.body01,
                        color = arrowLabelColor
                    )
                }
                if (arrowType == ArrowType.TITLE_ALIGNED) {
                    ArrowRight()
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing2x)
                )
            }
            if (footer != null) {
                Box(modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing8x)) { footer() }
            }
        }
        if (arrowType == ArrowType.CENTER_ALIGNED) ArrowRight()
    }
}

@Composable
private fun ArrowRight() {
    Box(
        modifier = Modifier.padding(
            start = MaterialTheme.wireDimensions.spacing8x,
        )
    ) { ArrowRightIcon(contentDescription = R.string.content_description_empty) }
}

enum class ArrowType {
    CENTER_ALIGNED, TITLE_ALIGNED, NONE
}

@Composable
@PreviewMultipleThemes
fun PreviewGroupConversationOptionsWithLabelAndTitle() = WireTheme {
    GroupConversationOptionsItem(title = "Conversation group title", label = "GROUP NAME")
}

@Composable
@PreviewMultipleThemes
fun PreviewGroupConversationOptionsWithTitleAndSwitchClickable() = WireTheme {
    GroupConversationOptionsItem(
        title = "Services",
        switchState = SwitchState.Enabled(value = true, onCheckedChange = {}),
        clickable = Clickable(onClick = {}, onLongClick = {})
    )
}

@Composable
@PreviewMultipleThemes
fun PreviewGroupConversationOptionsWithTitleAndSwitchWithoutArrow() = WireTheme {
    GroupConversationOptionsItem(
        title = "Services",
        switchState = SwitchState.Enabled(value = true, onCheckedChange = {}),
        arrowType = ArrowType.NONE
    )
}

@Composable
@PreviewMultipleThemes
fun PreviewGroupConversationOptionsWithTitleAndTextOnlySwitch() = WireTheme {
    GroupConversationOptionsItem(
        title = "Services",
        switchState = SwitchState.TextOnly(value = true),
        arrowType = ArrowType.NONE
    )
}

@Composable
@PreviewMultipleThemes
fun PreviewGroupConversationOptionsWithTitleAndSubtitleAndIcon() = WireTheme {
    GroupConversationOptionsItem(
        title = "Group Color",
        subtitle = "Red",
        titleTrailingItem = {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color = Color.Red, shape = RoundedCornerShape(8.dp))
            )
        },
        arrowType = ArrowType.TITLE_ALIGNED
    )
}

@Composable
@PreviewMultipleThemes
fun PreviewGroupConversationOptionsWithTitleAndSubtitleAndSwitchAndFooterButton() = WireTheme {
    GroupConversationOptionsItem(
        title = "Guests",
        subtitle = "Turn this option ON to open this conversation to people outside your team, even if they don't have Wire.",
        switchState = SwitchState.Disabled(false),
        footer = { WireSecondaryButton(text = "Copy link", onClick = {}, modifier = Modifier.height(32.dp), fillMaxWidth = false) },
        arrowType = ArrowType.TITLE_ALIGNED
    )
}

@Composable
@PreviewMultipleThemes
fun PreviewGroupConversationOptionsWithTitleAndSubtitleWithoutArrow() = WireTheme {
    GroupConversationOptionsItem(
        label = "Cipher Suite",
        title = "MLS_128_DHKEMX25519_AES128GCM_SHA256_Ed25519(0x0001)",
        titleStyle = MaterialTheme.wireTypography.body01,
        arrowType = ArrowType.NONE
    )
}

@Composable
@PreviewMultipleThemes
fun PreviewGroupConversationOptionsWithTitleAndArrowLabelAndRadioButton() = WireTheme {
    GroupConversationOptionsItem(
        title = "Custom",
        arrowLabel = "12 weeks",
        arrowType = ArrowType.TITLE_ALIGNED,
        selected = true,
    )
}
