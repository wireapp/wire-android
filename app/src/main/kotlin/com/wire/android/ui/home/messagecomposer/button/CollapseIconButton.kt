package com.wire.android.ui.home.messagecomposer.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun CollapseIconButton(onCollapseClick: () -> Unit, modifier: Modifier = Modifier, collapseRotation: Float = 0f) {
    IconButton(
        onClick = onCollapseClick,
        modifier = modifier.size(20.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_collapse),
            contentDescription = stringResource(R.string.content_description_drop_down_icon),
            tint = MaterialTheme.wireColorScheme.onSecondaryButtonDisabled,
            modifier = Modifier.rotate(collapseRotation)
        )
    }
}
