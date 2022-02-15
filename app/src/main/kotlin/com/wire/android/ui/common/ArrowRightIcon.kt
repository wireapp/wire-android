package com.wire.android.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions

@Composable
fun ArrowRightIcon(
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_arrow_right),
        contentDescription = "Right arrow",
        modifier = Modifier
            .size(MaterialTheme.wireDimensions.conversationBottomSheetItemSize)
            .then(modifier)
    )
}
