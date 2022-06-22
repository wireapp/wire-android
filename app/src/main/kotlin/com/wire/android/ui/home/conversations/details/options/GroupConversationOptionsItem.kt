package com.wire.android.ui.home.conversations.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.button.WireSecondaryButton
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
    enabled: Boolean? = null,
    titleStyle: TextStyle = MaterialTheme.wireTypography.body02,
    arrowType: ArrowType = ArrowType.CENTER_ALIGNED,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.wireColorScheme.surface)
            .let { if(onClick != null) it.clickable(onClick = onClick) else it }
            .padding(MaterialTheme.wireDimensions.spacing16x)
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
                if (enabled != null)
                    Text(
                        text = stringResource(if (enabled) R.string.label_on else R.string.label_off),
                        style = MaterialTheme.wireTypography.body01,
                        color = MaterialTheme.wireColorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing8x)
                    )
                if (arrowType == ArrowType.TITLE_ALIGNED) ArrowRight()
            }
            if (subtitle != null)
                Text(
                    text = subtitle,
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing8x)
                )
            if (footer != null)
                Box(modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing8x)) { footer() }
        }
        if (arrowType == ArrowType.CENTER_ALIGNED) ArrowRight()
    }
}

@Composable
private fun ArrowRight() {
    Box(modifier = Modifier.padding(start = MaterialTheme.wireDimensions.spacing8x)) { ArrowRightIcon() }
}

enum class ArrowType {
    CENTER_ALIGNED, TITLE_ALIGNED, NONE
}

@Composable
@Preview(name = "Item with label and title")
fun GroupConversationOptionsWithLabelAndTitlePreview() {
    GroupConversationOptionsItem(title = "Conversation group title", label = "GROUP NAME")
}

@Composable
@Preview(name = "Item with title and switch clickable")
fun GroupConversationOptionsWithTitleAndSwitchClickablePreview() {
    GroupConversationOptionsItem(title = "Services", enabled = true, onClick = {})
}

@Composable
@Preview(name = "Item with title, subtitle and icon")
fun GroupConversationOptionsWithTitleAndSubtitleAndIconPreview() {
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
fun GroupConversationOptionsWithTitleAndSubtitleAndSwitchAndFooterButtonPreview() {
    GroupConversationOptionsItem(
        title = "Guests",
        subtitle = "Turn this option ON to open this conversation to people outside your team, even if they don't have Wire.",
        enabled = false,
        footer = { WireSecondaryButton(text = "Copy link", onClick = {}, modifier = Modifier.height(32.dp), fillMaxWidth = false) },
        arrowType = ArrowType.TITLE_ALIGNED
    )
}

@Composable
@Preview(name = "Item with title and subtitle without arrow")
fun GroupConversationOptionsWithTitleAndSubtitleWithoutArrowPreview() {
    GroupConversationOptionsItem(
        label = "Cipher Suite",
        title = "MLS_128_DHKEMX25519_AES128GCM_SHA256_Ed25519(0x0001)",
        titleStyle = MaterialTheme.wireTypography.body01,
        arrowType = ArrowType.NONE
    )
}
