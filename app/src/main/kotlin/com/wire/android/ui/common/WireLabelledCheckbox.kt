package com.wire.android.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WireLabelledCheckbox(
    label: String,
    checked: Boolean,
    onCheckClicked: ((Boolean) -> Unit),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
        modifier = modifier
            .clickable(onClick = { onCheckClicked(!checked) })
            .requiredHeight(ButtonDefaults.MinHeight)
            .wrapContentWidth()
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null // null since we are handling the click on parent
        )

        Spacer(modifier = Modifier.size(MaterialTheme.wireDimensions.spacing8x))

        Text(
            text = label,
            style = MaterialTheme.wireTypography.body01
        )
    }
}
