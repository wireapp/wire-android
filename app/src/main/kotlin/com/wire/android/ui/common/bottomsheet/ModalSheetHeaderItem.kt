package com.wire.android.ui.common.bottomsheet

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun ModalSheetHeaderItem(header: MenuModalSheetHeader = MenuModalSheetHeader.Gone) {
    when (header) {
        MenuModalSheetHeader.Gone -> {
            Spacer(modifier = Modifier.height(dimensions().modalBottomSheetNoHeaderVerticalPadding))
        }
        is MenuModalSheetHeader.Visible -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    start = dimensions().modalBottomSheetHeaderHorizontalPadding,
                    end = dimensions().modalBottomSheetHeaderHorizontalPadding,
                    top = dimensions().modalBottomSheetHeaderVerticalPadding,
                    bottom = header.customBottomPadding ?: dimensions().modalBottomSheetHeaderVerticalPadding
                )
            ) {
                header.leadingIcon()
                Spacer(modifier = Modifier.width(dimensions().spacing8x))
                Text(
                    text = header.title,
                    style = MaterialTheme.wireTypography.title02
                )
            }
        }
    }
}

sealed class MenuModalSheetHeader {

    data class Visible (
        val title: String,
        val leadingIcon: @Composable () -> Unit = {},
        val customBottomPadding: Dp? = null
    ) : MenuModalSheetHeader()

    object Gone : MenuModalSheetHeader()
}
