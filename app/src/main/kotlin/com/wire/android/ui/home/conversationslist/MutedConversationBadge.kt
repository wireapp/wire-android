package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions

@Composable
fun MutedConversationBadge(onClick: () -> Unit) {
    WireSecondaryButton(
        onClick = onClick,
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_mute),
                contentDescription = stringResource(R.string.content_description_mute),
                modifier = Modifier.size(dimensions().spacing16x)
            )
        },
        fillMaxWidth = false,
        minHeight = dimensions().badgeSmallMinSize.height,
        minWidth = dimensions().badgeSmallMinSize.width,
        shape = RoundedCornerShape(size = dimensions().spacing6x),
        contentPadding = PaddingValues(0.dp),
    )
}
