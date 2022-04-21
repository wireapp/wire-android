package com.wire.android.ui.common.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import io.github.esentsov.PackagePrivate

@Composable
fun RichMenuBottomSheetItem(
    title: String,
    subLine: String,
    icon: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    onItemClick: () -> Unit = {},
    state: RichMenuItemState = RichMenuItemState.DEFAULT
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (isSelectedItem(state)) Modifier.background(MaterialTheme.wireColorScheme.secondaryButtonSelected) else Modifier
    ) {
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .wrapContentWidth()
                .clickable { onItemClick() }
        ) {
            if (icon != null) {
                icon()
            }
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .padding(dimensions().spacing12x)
                    .height(dimensions().spacing64x)
                    .weight(1f),
            ) {
                Spacer(modifier = Modifier.width(dimensions().spacing12x))
                MenuItemHeading(title = title, state = state)
                Spacer(modifier = Modifier.height(dimensions().spacing8x))
                MenuItemSubLine(subLine = subLine)
            }
            if (action != null) {
                Column(
                    modifier = Modifier
                        .padding(dimensions().spacing8x)
                        .align(Alignment.CenterVertically)
                ) {
                    action()
                }
            }
        }
    }
}

@PackagePrivate
@Composable
fun MenuItemHeading(
    title: String,
    state: RichMenuItemState = RichMenuItemState.DEFAULT,
    modifier: Modifier = Modifier
) {
    Text(
        style = MaterialTheme.wireTypography.body02,
        color = if (isSelectedItem(state)) MaterialTheme.wireColorScheme.primary else MaterialTheme.wireColorScheme.onBackground,
        text = title,
        modifier = modifier.fillMaxWidth()
    )
}

@PackagePrivate
@Composable
fun MenuItemSubLine(
    subLine: String,
    modifier: Modifier = Modifier
) {
    Text(
        style = MaterialTheme.wireTypography.subline01,
        color = MaterialTheme.wireColorScheme.labelText,
        text = subLine,
        modifier = modifier.fillMaxWidth()
    )
}

private fun isSelectedItem(state: RichMenuItemState) = state == RichMenuItemState.SELECTED

enum class RichMenuItemState {
    DEFAULT, SELECTED
}
