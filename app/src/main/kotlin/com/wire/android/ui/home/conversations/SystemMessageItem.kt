package com.wire.android.ui.home.conversations

import android.content.res.Resources
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.MessageContent.SystemMessage
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.android.util.ui.toUIText

@Composable
fun SystemMessageItem(message: SystemMessage) {
    val fullAvatarOuterPadding = dimensions().userAvatarClickablePadding + dimensions().userAvatarStatusBorderSize
    Row(
        Modifier
            .padding(
                end = dimensions().spacing16x,
                start = dimensions().spacing8x,
                top = fullAvatarOuterPadding,
                bottom = dimensions().messageItemBottomPadding - fullAvatarOuterPadding
            )
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .width(dimensions().userAvatarDefaultSize),
            contentAlignment = Alignment.TopEnd
        ) {
            if (message.iconResId != null) {
                Box(
                    modifier = Modifier.size(
                        width = dimensions().systemMessageIconLargeSize,
                        height =  dimensions().spacing20x
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    val size =
                        if (message.isSmallIcon) dimensions().systemMessageIconSize
                        else dimensions().systemMessageIconLargeSize
                    Image(
                        painter = painterResource(id = message.iconResId),
                        contentDescription = stringResource(R.string.content_description_system_message_icon),
                        modifier = Modifier.size(size),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        Spacer(Modifier.padding(start = dimensions().spacing16x))
        Column {
            val context = LocalContext.current
            var expanded: Boolean by remember { mutableStateOf(false) }
            Crossfade(targetState = expanded) {
                Text(
                    modifier = Modifier.defaultMinSize(minHeight = dimensions().spacing20x),
                    style = MaterialTheme.wireTypography.body01,
                    lineHeight = MaterialTheme.wireTypography.body02.lineHeight,
                    text = message.annotatedString(
                        context.resources,
                        it,
                        MaterialTheme.wireTypography.body01,
                        MaterialTheme.wireTypography.body02,
                        MaterialTheme.wireColorScheme.secondaryText,
                        MaterialTheme.wireColorScheme.onBackground,
                    )
                )
            }
            if (message.expandable) {
                WireSecondaryButton(
                    onClick = { expanded = !expanded },
                    text = stringResource(if (expanded) R.string.label_show_less else R.string.label_show_all),
                    fillMaxWidth = false,
                    minHeight = dimensions().spacing32x,
                    minWidth = dimensions().spacing40x,
                    shape = RoundedCornerShape(size = dimensions().corner12x),
                    contentPadding = PaddingValues(horizontal = dimensions().spacing12x, vertical = dimensions().spacing8x),
                    modifier = Modifier
                        .padding(top = dimensions().spacing4x)
                        .height(height = dimensions().spacing32x)
                )
            }
        }
    }
}

@Preview
@Composable
fun SystemMessageAdded7UsersPreview() {
    SystemMessageItem(
        message = SystemMessage.MemberAdded(
            "Barbara Cotolina".toUIText(),
            listOf(
                "Albert Lewis".toUIText(), "Bert Strunk".toUIText(), "Claudia Schiffer".toUIText(), "Dorothee Friedrich".toUIText(),
                "Erich Weinert".toUIText(), "Frieda Kahlo".toUIText(), "Gudrun Gut".toUIText()
            )
        )
    )
}

@Preview
@Composable
fun SystemMessageAdded4UsersPreview() {
    SystemMessageItem(
        message = SystemMessage.MemberAdded(
            "Barbara Cotolina".toUIText(),
            listOf("Albert Lewis".toUIText(), "Bert Strunk".toUIText(), "Claudia Schiffer".toUIText(), "Dorothee Friedrich".toUIText())
        )
    )
}

@Preview
@Composable
fun SystemMessageRemoved4UsersPreview() {
    SystemMessageItem(
        message = SystemMessage.MemberRemoved(
            "Barbara Cotolina".toUIText(),
            listOf("Albert Lewis".toUIText(), "Bert Strunk".toUIText(), "Claudia Schiffer".toUIText(), "Dorothee Friedrich".toUIText())
        )
    )
}

@Preview
@Composable
fun SystemMessageLeftPreview() {
    SystemMessageItem(message = SystemMessage.MemberLeft(UIText.DynamicString("Barbara Cotolina")))
}

@Preview
@Composable
fun SystemMessageMissedCallPreview() {
    SystemMessageItem(message = SystemMessage.MissedCall.OtherCalled(UIText.DynamicString("Barbara Cotolina")))
}

private val SystemMessage.expandable
    get() = when (this) {
        is SystemMessage.MemberAdded -> this.memberNames.size > EXPANDABLE_THRESHOLD
        is SystemMessage.MemberRemoved -> this.memberNames.size > EXPANDABLE_THRESHOLD
        is SystemMessage.MemberLeft -> false
        is SystemMessage.MissedCall -> false
    }

private fun List<String>.toUserNamesListString(res: Resources) = when {
    this.isEmpty() -> ""
    this.size == 1 -> this[0]
    else -> res.getString(R.string.label_system_message_and, this.dropLast(1).joinToString(", "), this.last())
}

private fun List<UIText>.limitUserNamesList(res: Resources, threshold: Int): List<String> =
    if (this.size <= threshold) this.map { it.asString(res) }
    else {
        val moreCount = this.size - (threshold - 1) // the last visible place is taken by "and X more"
        this.take(threshold - 1)
            .map { it.asString(res) }
            .plus(res.getQuantityString(R.plurals.label_system_message_x_more, moreCount, moreCount))
    }

@Suppress("LongParameterList", "SpreadOperator")
fun SystemMessage.annotatedString(
    res: Resources,
    expanded: Boolean,
    normalStyle: TextStyle,
    boldStyle: TextStyle,
    normalColor: Color,
    boldColor: Color
): AnnotatedString {
    val args = when (this) {
        is SystemMessage.MemberAdded ->
            arrayOf(
                author.asString(res),
                memberNames.limitUserNamesList(res, if (expanded) memberNames.size else EXPANDABLE_THRESHOLD).toUserNamesListString(res)
            )
        is SystemMessage.MemberRemoved ->
            arrayOf(
                author.asString(res),
                memberNames.limitUserNamesList(res, if (expanded) memberNames.size else EXPANDABLE_THRESHOLD).toUserNamesListString(res)
            )
        is SystemMessage.MemberLeft -> arrayOf(author.asString(res))
        is SystemMessage.MissedCall -> arrayOf(author.asString(res))
    }
    return res.stringWithStyledArgs(stringResId, normalStyle, boldStyle, normalColor, boldColor, *args)
}

private const val EXPANDABLE_THRESHOLD = 4
