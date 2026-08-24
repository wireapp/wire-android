/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.newauthentication.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.wire.android.ui.common.bottomsheet.WireBottomSheetDefaults
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconButton
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.wireTypography

/**
 * Shared new-authentication shell. Host-specific controls (for example the private
 * backend selector) are deliberately supplied through [topBar].
 */
@Composable
fun NewAuthContainer(
    header: @Composable () -> Unit = {},
    contentPadding: Dp = dimensions().spacing24x,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    NavigationBarBackground()
    WireScaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = topBar,
        bottomBar = {
            Column(
                modifier = Modifier
                    .clip(WireBottomSheetDefaults.WireBottomSheetShape)
                    .background(WireBottomSheetDefaults.WireSheetContainerColor),
            ) {
                Surface(
                    color = WireBottomSheetDefaults.WireSheetContainerColor,
                    shadowElevation = scrollState.rememberTopBarElevationState().value,
                    modifier = Modifier.zIndex(1f),
                ) {
                    header()
                }
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(start = contentPadding, end = contentPadding, bottom = contentPadding),
                    content = content,
                )
            }
        },
    ) { _ -> }
}

@Composable
fun NewAuthHeader(
    title: @Composable ColumnScope.() -> Unit,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions().spacing12x),
        verticalAlignment = Alignment.Top,
    ) {
        if (canNavigateBack) {
            NavigationIconButton(
                iconType = NavigationIconType.Back(),
                onClick = onNavigateBack,
                modifier = Modifier
                    .padding(vertical = dimensions().spacing12x)
                    .size(dimensions().spacing48x),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .defaultMinSize(minHeight = dimensions().spacing48x)
                .padding(horizontal = dimensions().spacing12x, vertical = dimensions().spacing24x)
                .weight(1f),
            content = title,
        )
        if (canNavigateBack) {
            // Keep the title centred relative to the full header, not the remaining space.
            Box(modifier = Modifier.size(dimensions().spacing48x))
        }
    }
}

@Composable
fun NewAuthTitle(
    title: String,
    verticalPadding: Dp = dimensions().spacing2x,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = typography().title01,
        color = colorsScheme().onSurface,
        textAlign = TextAlign.Center,
        modifier = modifier
            .padding(vertical = verticalPadding)
            .semantics { heading() },
    )
}

@Composable
fun NewAuthSubtitle(
    title: String,
    verticalPadding: Dp = dimensions().spacing2x,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.wireTypography.body01,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(vertical = verticalPadding),
    )
}

@Composable
private fun NavigationBarBackground() = Box(
    contentAlignment = Alignment.BottomCenter,
    modifier = Modifier.fillMaxSize(),
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorsScheme().background)
            .navigationBarsPadding(),
    )
}
