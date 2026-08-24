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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    topBarTitle: String,
    message: AnnotatedString,
    icon: Painter,
    onSyncCompleted: (shouldMoveToBackground: Boolean) -> Unit,
) {
    InitialSyncProgressContent(topBarTitle, message, icon)

    val state = viewModel.state
    LaunchedEffect(state) {
        if (state is InitialSyncState.Completed) onSyncCompleted(state.shouldMoveToBackground)
    }
}

@Composable
private fun InitialSyncProgressContent(
    topBarTitle: String,
    message: AnnotatedString,
    icon: Painter,
) {
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = MaterialTheme.wireDimensions.spacing0x,
                title = topBarTitle,
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
                    painter = icon,
                    contentDescription = null,
                    contentScale = ContentScale.Inside,
                    modifier = Modifier.padding(
                        horizontal = MaterialTheme.wireDimensions.welcomeImageHorizontalPadding,
                        vertical = MaterialTheme.wireDimensions.welcomeVerticalSpacing,
                    ),
                )
                WireCircularProgressIndicator(progressColor = MaterialTheme.wireColorScheme.onBackground)
                Text(
                    text = message,
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
