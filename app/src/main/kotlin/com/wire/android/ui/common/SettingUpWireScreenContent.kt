package com.wire.android.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingUpWireScreenContent(
    @StringRes titleResId: Int = R.string.migration_title,
    @StringRes messageResId: Int = R.string.migration_message,
    @DrawableRes iconResId: Int = R.drawable.ic_migration,
) {
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = stringResource(titleResId),
                navigationIconType = null,
            )
        }
    ) { internalPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            Image(
                painter = painterResource(iconResId),
                contentDescription = "",
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .padding(
                        horizontal = MaterialTheme.wireDimensions.welcomeImageHorizontalPadding,
                        vertical = MaterialTheme.wireDimensions.welcomeVerticalSpacing
                    )
            )
            WireCircularProgressIndicator(progressColor = MaterialTheme.wireColorScheme.badge)
            Text(
                text = stringResource(messageResId),
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.welcomeTextHorizontalPadding)
            )
        }
    }
}

@Preview
@Composable
private fun MigrationScreenPreview() {
    SettingUpWireScreenContent()
}

