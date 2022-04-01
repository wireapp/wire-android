package com.wire.android.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.button.IconAlignment
import com.wire.android.ui.common.button.WireSecondaryButton

@Composable
fun MoreOptionIcon(
    onButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireSecondaryButton(
        onClick = onButtonClicked,
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_more),
                contentDescription = stringResource(R.string.content_description_right_arrow),
                modifier = modifier
                    .size(dimensions().conversationBottomSheetItemSize)
            )
        },
        minHeight = 32.dp,
        minWidth = 40.dp,
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
        leadingIconAlignment = IconAlignment.Center,
        fillMaxWidth = false
    )
}
