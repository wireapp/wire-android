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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.WireSwitch
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.clickable
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun GroupConversationOptionsItem(
    title: String,
    subtitle: String? = null,
    label: String? = null,
    titleTrailingItem: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    switchState: SwitchState = SwitchState.None,
    titleStyle: TextStyle = MaterialTheme.wireTypography.body02,
    arrowType: ArrowType = ArrowType.CENTER_ALIGNED,
    clickable: Clickable = Clickable(enabled = false, onClick = { /* not handled */ }, onLongClick = { /* not handled */ }),
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.wireColorScheme.surface)
        .clickable(clickable)
        .defaultMinSize(minHeight = MaterialTheme.wireDimensions.conversationOptionsItemMinHeight)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    top = MaterialTheme.wireDimensions.spacing12x,
                    bottom = MaterialTheme.wireDimensions.spacing12x,
                    start = MaterialTheme.wireDimensions.spacing16x,
                    end = MaterialTheme.wireDimensions.spacing12x
                )
        ) {
            if (label != null)
                Text(
                    text = label,
                    style = MaterialTheme.wireTypography.label01,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.spacing4x)
                )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = titleStyle,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    modifier = Modifier.weight(weight = 1f, fill = true)
                )
                if (titleTrailingItem != null)
                    Box(modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing8x)) { titleTrailingItem() }
                if (switchState is SwitchState.Visible) {
                    if (switchState.isOnOffVisible) {
                        Text(
                            text = stringResource(if (switchState.value) R.string.label_on else R.string.label_off),
                            style = MaterialTheme.wireTypography.body01,
                            color = MaterialTheme.wireColorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing8x)
                        )
                    }
                    if (switchState.isSwitchVisible) {
                        WireSwitch(
                            checked = switchState.value,
                            enabled = switchState is SwitchState.Enabled,
                            onCheckedChange = (switchState as? SwitchState.Enabled)?.onCheckedChange
                        )
                    }
                }
                if (arrowType == ArrowType.TITLE_ALIGNED) ArrowRight()
            }
            if (subtitle != null)
                Text(
                    text = subtitle,
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing2x)
                )
            if (footer != null)
                Box(modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing8x)) { footer() }
        }
        if (arrowType == ArrowType.CENTER_ALIGNED) ArrowRight()
    }
}

@Composable
private fun ArrowRight() {
    Box(
        modifier = Modifier.padding(
            start = MaterialTheme.wireDimensions.spacing8x,
            end = MaterialTheme.wireDimensions.spacing8x
        )
    ) { ArrowRightIcon() }
}

enum class ArrowType {
    CENTER_ALIGNED, TITLE_ALIGNED, NONE
}

sealed class SwitchState {
    object None : SwitchState()
    sealed class Visible(
        open val value: Boolean = false,
        open val isOnOffVisible: Boolean = true,
        open val isSwitchVisible: Boolean = true
    ) : SwitchState()

    data class Enabled(
        override val value: Boolean = false,
        override val isOnOffVisible: Boolean = true,
        val onCheckedChange: (Boolean) -> Unit
    ) : Visible(value = value, isSwitchVisible = true)

    data class Disabled(
        override val value: Boolean = false,
        override val isOnOffVisible: Boolean = true
    ) : Visible(value, isSwitchVisible = true)

    data class TextOnly(
        override val value: Boolean = false,
    ) : Visible(value = value, isOnOffVisible = true, isSwitchVisible = false)
}

@Composable
@Preview(name = "Item with label and title")
private fun GroupConversationOptionsWithLabelAndTitlePreview() {
    GroupConversationOptionsItem(title = "Conversation group title", label = "GROUP NAME")
}

@Composable
@Preview(name = "Item with title and switch clickable")
private fun GroupConversationOptionsWithTitleAndSwitchClickablePreview() {
    GroupConversationOptionsItem(
        title = "Services",
        switchState = SwitchState.Enabled(value = true, onCheckedChange = {}),
        clickable = Clickable(onClick = {}, onLongClick = {})
    )
}

@Composable
@Preview(name = "Item with title and text only switch")
private fun GroupConversationOptionsWithTitleAndTextOnlySwitchPreview() {
    GroupConversationOptionsItem(
        title = "Services",
        switchState = SwitchState.TextOnly(value = true),
        arrowType = ArrowType.NONE
    )
}

@Composable
@Preview(name = "Item with title, subtitle and icon")
private fun GroupConversationOptionsWithTitleAndSubtitleAndIconPreview() {
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
@Preview(name = "Item with title, subtitle, switch and footer button")
private fun GroupConversationOptionsWithTitleAndSubtitleAndSwitchAndFooterButtonPreview() {
    GroupConversationOptionsItem(
        title = "Guests",
        subtitle = "Turn this option ON to open this conversation to people outside your team, even if they don't have Wire.",
        switchState = SwitchState.Disabled(false),
        footer = { WireSecondaryButton(text = "Copy link", onClick = {}, modifier = Modifier.height(32.dp), fillMaxWidth = false) },
        arrowType = ArrowType.TITLE_ALIGNED
    )
}

@Composable
@Preview(name = "Item with title and subtitle without arrow")
private fun GroupConversationOptionsWithTitleAndSubtitleWithoutArrowPreview() {
    GroupConversationOptionsItem(
        label = "Cipher Suite",
        title = "MLS_128_DHKEMX25519_AES128GCM_SHA256_Ed25519(0x0001)",
        titleStyle = MaterialTheme.wireTypography.body01,
        arrowType = ArrowType.NONE
    )
}


