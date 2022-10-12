package com.wire.android.ui.migration

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.WireCircularProgressIndicator
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun MigrationScreen(viewModel: MigrationViewModel = hiltViewModel()) {
    viewModel.init()
    MigrationScreenContent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MigrationScreenContent() {
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = stringResource(R.string.migration_title),
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
                painter = painterResource(id = R.drawable.ic_migration),
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
                text = stringResource(R.string.migration_message),
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
    MigrationScreenContent()
}
