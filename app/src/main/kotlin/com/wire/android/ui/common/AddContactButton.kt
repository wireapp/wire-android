package com.wire.android.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.button.IconAlignment
import com.wire.android.ui.common.button.WireSecondaryButton

@Composable
fun AddContactButton(
    onIconClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireSecondaryButton(
        onClick = { onIconClicked() },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_contact),
                contentDescription = stringResource(R.string.content_description_add_contact),
            )
        },
        leadingIconAlignment = IconAlignment.Center,
        //TODO: remove this after the merge into the develop as they are not needed anymore
        fillMaxWidth = false,
        minHeight = 32.dp,
        minWidth = 40.dp,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = modifier
    )
}
