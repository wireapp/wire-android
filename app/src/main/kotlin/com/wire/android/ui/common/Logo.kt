package com.wire.android.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R

@Composable
fun Logo() {
    Image(
        painter = painterResource(id = R.drawable.ic_wire_logo),
        contentDescription = stringResource(id = R.string.app_logo_description),
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(80.dp)
            .height(30.dp)
            .padding(8.dp)
    )
}
