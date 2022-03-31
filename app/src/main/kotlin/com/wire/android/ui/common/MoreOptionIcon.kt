package com.wire.android.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions

@Composable
fun MoreOptionIcon(
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_more),
        contentDescription = stringResource(R.string.content_description_more_options),
        modifier = Modifier
            .size(MaterialTheme.wireDimensions.conversationBottomSheetItemSize)
            .then(modifier)
    )
}
