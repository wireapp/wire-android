package com.wire.android.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.wire.android.R

@Composable
fun AddContactIcon(onIconClicked: () -> Unit, modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(id = R.drawable.ic_add_contact),
        contentDescription = "",
        modifier = Modifier
            .clickable { onIconClicked() }
            .then(modifier)
    )
}
