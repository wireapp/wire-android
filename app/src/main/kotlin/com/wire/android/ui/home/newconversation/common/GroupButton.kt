package com.wire.android.ui.home.newconversation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.button.IconAlignment
import com.wire.android.ui.common.button.WireTertiaryButton
import com.wire.android.ui.common.button.wireSecondaryButtonColors
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.theme.wireDimensions

@Composable
fun GroupButton(groupSize: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(82.dp)
            .fillMaxWidth()
            .padding(all = MaterialTheme.wireDimensions.spacing16x)
    ) {
        WirePrimaryButton(
            text = "${stringResource(R.string.label_new_group)} (${groupSize})",
            onClick = {
                //TODO:open new group screen
            },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(dimensions().spacing8x))
        WireTertiaryButton(
            colors = wireSecondaryButtonColors(),
            onClick = { },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_more),
                    contentDescription = stringResource(R.string.content_description_right_arrow),
                    modifier = Modifier
                        .size(MaterialTheme.wireDimensions.conversationBottomSheetItemSize)
                )
            },
            leadingIconAlignment = IconAlignment.Center,
            fillMaxWidth = false
        )
    }
}
