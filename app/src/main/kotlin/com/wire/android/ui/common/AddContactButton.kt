package com.wire.android.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.button.IconAlignment
import com.wire.android.ui.common.button.WireSecondaryButton

@Composable
fun AddContactButton(onIconClicked: () -> Unit, modifier: Modifier = Modifier) {
    WireSecondaryButton(
        onClick = { onIconClicked() },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_contact),
                contentDescription = "",
                modifier = Modifier
                    .clickable { onIconClicked() }
                    .then(modifier)
            )
        },
        leadingIconAlignment = IconAlignment.Center,
        fillMaxWidth = false,
        minHeight = 32.dp,
        minWidth = 40.dp,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    )
}
