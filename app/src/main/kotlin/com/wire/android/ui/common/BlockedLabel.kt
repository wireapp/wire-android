package com.wire.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun BlockedLabel(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.border(
            width = MaterialTheme.wireDimensions.spacing1x,
            shape = RoundedCornerShape(MaterialTheme.wireDimensions.spacing4x),
            color = MaterialTheme.wireColorScheme.secondaryButtonDisabledOutline
        )
    ) {
        Text(
            text = stringResource(id = R.string.label_user_blocked),
            color = MaterialTheme.wireColorScheme.labelText,
            style = MaterialTheme.wireTypography.label03.copy(textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .wrapContentWidth()
                .background(
                    color = MaterialTheme.wireColorScheme.secondaryButtonDisabled,
                    shape = RoundedCornerShape(MaterialTheme.wireDimensions.corner4x)
                )
                .padding(horizontal = MaterialTheme.wireDimensions.spacing4x, vertical = MaterialTheme.wireDimensions.spacing2x)
        )
    }
}

@Preview
@Composable
fun BlockedLabelPreview() {
    BlockedLabelPreview()
}
