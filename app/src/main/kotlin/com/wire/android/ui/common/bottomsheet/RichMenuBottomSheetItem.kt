package com.wire.android.ui.common.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.model.Clickable
import com.wire.android.ui.common.WireCheckIcon
import com.wire.android.ui.common.clickable
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.DEFAULT_WEIGHT
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import io.github.esentsov.PackagePrivate

@Composable
fun RichMenuBottomSheetItem(
    title: String,
    subLine: String? = null,
    icon: @Composable () -> Unit = { },
    action: @Composable () -> Unit = { },
    onItemClick: Clickable = Clickable(enabled = false) {},
    state: RichMenuItemState = RichMenuItemState.DEFAULT
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .defaultMinSize(minHeight = dimensions().spacing48x)
            .let { if (isSelectedItem(state)) it.background(MaterialTheme.wireColorScheme.secondaryButtonSelected) else it }
            .clickable(onItemClick)
            .padding(vertical = dimensions().spacing12x, horizontal = dimensions().spacing16x)
    ) {
        icon()
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(DEFAULT_WEIGHT),
        ) {
            MenuItemHeading(title = title, state = state)
            if (subLine != null) {
                MenuItemSubLine(
                    subLine = subLine,
                    modifier = Modifier.padding(top = dimensions().spacing8x)
                )
            }
        }
        if (isSelectedItem(state)) {
            Column(
                modifier = Modifier
                    .padding(start = dimensions().spacing8x)
                    .align(Alignment.CenterVertically)
            ) {
                action()
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

@Composable
@Preview
fun RichMenuBottomSheetItemPreview() {
    RichMenuBottomSheetItem("title", "subLine", { WireCheckIcon() }, {}, Clickable {}, RichMenuItemState.SELECTED)
}
