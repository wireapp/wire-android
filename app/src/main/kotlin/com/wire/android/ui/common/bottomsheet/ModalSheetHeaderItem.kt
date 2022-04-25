package com.wire.android.ui.common.bottomsheet

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun ModalSheetHeaderItem(
    title: String? = null,
    leadingIcon: @Composable () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(
                start = dimensions().modalBottomSheetHeaderStartPadding,
                top = dimensions().modalBottomSheetHeaderTopPadding,
                bottom = dimensions().modalBottomSheetHeaderBottomPadding
            )
    ) {
        leadingIcon()
        Spacer(modifier = Modifier.width(8.dp))
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.wireTypography.title02
            )
        }
    }
}
