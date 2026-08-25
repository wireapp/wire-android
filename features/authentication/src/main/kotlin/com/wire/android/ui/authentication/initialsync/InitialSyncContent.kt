/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication.initialsync

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

/**
 * The initial-sync surface intentionally remains non-dismissible. The host owns the terminal
 * Navigation 3 transition; this content emits it only after the gateway has awaited persistence.
 */
@Composable
fun InitialSyncRouteContent(
    viewModel: InitialSyncViewModel,
    onSyncCompleted: (shouldMoveToBackground: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    InitialSyncProgressContent(modifier)

    val state = viewModel.state
    LaunchedEffect(state) {
        if (state is InitialSyncState.Completed) onSyncCompleted(state.shouldMoveToBackground)
    }
}

@Composable
private fun InitialSyncProgressContent(
    modifier: Modifier = Modifier,
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = MaterialTheme.wireDimensions.spacing0x,
                title = stringResource(AuthenticationR.string.migration_title),
                navigationIconType = null,
            )
        },
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = true),
            ) {
                Image(
                    painter = painterResource(AuthenticationR.drawable.ic_migration),
                    contentDescription = null,
                    contentScale = ContentScale.Inside,
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.wireDimensions.welcomeImageHorizontalPadding,
                        vertical = MaterialTheme.wireDimensions.welcomeVerticalSpacing,
                    ),
                )
                WireCircularProgressIndicator(progressColor = MaterialTheme.wireColorScheme.onBackground)
                Text(
                    text = AnnotatedString(stringResource(AuthenticationR.string.migration_message)),
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.wireDimensions.welcomeTextHorizontalPadding,
                    ),
                )
            }
        }
    }
}
