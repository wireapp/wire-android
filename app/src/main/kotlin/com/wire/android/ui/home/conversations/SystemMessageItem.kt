package com.wire.android.ui.home.conversations

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun SystemMessageItem(type: SystemMessageType) {
    Row(
        Modifier
            .padding(
                end = dimensions().spacing16x,
                start = dimensions().spacing8x,
                top = dimensions().messageItemBottomPadding / 2,
                bottom = dimensions().messageItemBottomPadding / 2
            )
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.width(dimensions().userAvatarDefaultSize),
            contentAlignment = Alignment.TopEnd
        ) {
            if (type.iconResId != null)
                Image(
                    painter = painterResource(id = type.iconResId),
                    contentDescription = stringResource(R.string.content_description_system_message_icon),
                    modifier = Modifier
                        .padding(
                            horizontal = dimensions().spacing2x,
                            vertical = dimensions().spacing4x
                        )
                        .size(dimensions().systemMessageIconSize),
                    contentScale = ContentScale.Crop
                )
        }
        Spacer(Modifier.padding(start = dimensions().spacing16x))
        Column {
            val context = LocalContext.current
            val normalStyle = SpanStyle(
                color = MaterialTheme.wireColorScheme.secondaryText,
                fontWeight = MaterialTheme.wireTypography.body01.fontWeight,
                fontSize = MaterialTheme.wireTypography.body01.fontSize,
                fontFamily = MaterialTheme.wireTypography.body01.fontFamily,
                fontStyle = MaterialTheme.wireTypography.body01.fontStyle
            )
            val boldStyle = SpanStyle(
                color = MaterialTheme.wireColorScheme.onBackground,
                fontWeight = MaterialTheme.wireTypography.body02.fontWeight,
                fontSize = MaterialTheme.wireTypography.body02.fontSize,
                fontFamily = MaterialTheme.wireTypography.body02.fontFamily,
                fontStyle = MaterialTheme.wireTypography.body02.fontStyle
            )
            var expanded: Boolean by remember { mutableStateOf(false) }
            Crossfade(targetState = expanded) {
                Text(type.getAnnotatedString(context.resources, it, normalStyle, boldStyle))
            }
            if (type.expandable) {
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
fun SystemMessageItemAdded7UsersPreview() {
    SystemMessageItem(
        type = SystemMessageType.Added(
            "Barbara Cotolina",
            listOf("Albert Lewis", "Bert Strunk", "Claudia Schiffer", "Dorothee Friedrich", "Erich Weinert", "Frieda Kahlo", "Gudrun Gut")
        )
    )
}

@Preview
@Composable
fun SystemMessageItemAdded4UsersPreview() {
    SystemMessageItem(
        type = SystemMessageType.Added(
            "Barbara Cotolina",
            listOf("Albert Lewis", "Bert Strunk", "Claudia Schiffer", "Dorothee Friedrich")
        )
    )
}

@Preview
@Composable
fun SystemMessageItemRemoved4UsersPreview() {
    SystemMessageItem(
        type = SystemMessageType.Removed(
            "Barbara Cotolina",
            listOf("Albert Lewis", "Bert Strunk", "Claudia Schiffer", "Dorothee Friedrich")
        )
    )
}

@Preview
@Composable
fun SystemMessageItemLeftPreview() {
    SystemMessageItem(type = SystemMessageType.Left("Barbara Cotolina"))
}

sealed class SystemMessageType(@DrawableRes val iconResId: Int?, @StringRes val stringResId: Int) {
    data class Added(
        val author: String,
        val userNames: List<String>
    ) : SystemMessageType(R.drawable.ic_add, R.string.label_system_message_added)

    data class Removed(
        val author: String,
        val userNames: List<String>
    ) : SystemMessageType(R.drawable.ic_minus, R.string.label_system_message_removed)

    data class Left(
        val author: String
    ) : SystemMessageType(R.drawable.ic_minus, R.string.label_system_message_left_the_conversation)
}

val SystemMessageType.expandable
    get() = when (this) {
        is SystemMessageType.Added -> this.userNames.size > EXPANDABLE_THRESHOLD
        is SystemMessageType.Removed -> this.userNames.size > EXPANDABLE_THRESHOLD
        is SystemMessageType.Left -> false
    }

private fun String.bold() = STYLE_SEPARATOR + this + STYLE_SEPARATOR

private fun List<String>.toUserNamesListString(res: Resources) = when {
    this.isEmpty() -> ""
    this.size == 1 -> this[0].bold()
    else -> res.getString(R.string.label_system_message_and, this.dropLast(1).joinToString(", ").bold(), this.last().bold())
}

private fun List<String>.limitUserNamesList(res: Resources, threshold: Int) =
    if (this.size <= threshold) this
    else {
        val moreCount = this.size - (threshold - 1) // the last visible place is taken by "and X more"
        this.take(threshold - 1)
            .plus(res.getQuantityString(R.plurals.label_system_message_x_more, moreCount, moreCount))
    }

fun SystemMessageType.getAnnotatedString(res: Resources, expanded: Boolean, normalStyle: SpanStyle, boldStyle: SpanStyle): AnnotatedString {
    val string = when (this) {
        is SystemMessageType.Added -> res.getString(
            stringResId,
            author.bold(),
            userNames.limitUserNamesList(res, if (expanded) userNames.size else EXPANDABLE_THRESHOLD).toUserNamesListString(res)
        )
        is SystemMessageType.Removed -> res.getString(
            stringResId,
            author.bold(),
            userNames.limitUserNamesList(res, if (expanded) userNames.size else EXPANDABLE_THRESHOLD).toUserNamesListString(res)
        )
        is SystemMessageType.Left -> res.getString(stringResId, author.bold())
    }
    return buildAnnotatedString {
        string.split(STYLE_SEPARATOR).forEachIndexed { index, text ->
            withStyle(if (index % 2 == 0) normalStyle else boldStyle) { append(text) }
        }
    }
}

private const val EXPANDABLE_THRESHOLD = 4
private const val STYLE_SEPARATOR: String = "\u0000"
