package com.wire.android.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.theme.wireDimensions

@Composable
fun Logo() {
    Image(
        painter = painterResource(id = R.drawable.ic_wire_logo),
        contentDescription = stringResource(id = R.string.app_logo_description),
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .padding(
                horizontal = MaterialTheme.wireDimensions.homeDrawerLogoHorizontalPadding,
                vertical = MaterialTheme.wireDimensions.homeDrawerLogoVerticalPadding
            )
            .width(MaterialTheme.wireDimensions.homeDrawerLogoWidth)
            .height(MaterialTheme.wireDimensions.homeDrawerLogoHeight)
    )
}
