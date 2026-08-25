/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.authentication.create.overview

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.feature.authentication.R
import com.wire.android.ui.common.R as CommonR
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun CreateAccountOverviewContent(
    overviewParams: CreateAccountOverviewParams,
    continueText: String,
    @StringRes backContentDescription: Int,
    onBackPressed: () -> Unit,
    onContinuePressed: () -> Unit,
    onLearnMorePressed: (String) -> Unit,
    modifier: Modifier = Modifier,
    subtitleContent: @Composable ColumnScope.() -> Unit = {},
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = dimensions().spacing0x,
                title = overviewParams.title,
                onNavigationPressed = onBackPressed,
                navigationIconType = NavigationIconType.Back(backContentDescription),
                subtitleContent = subtitleContent,
            )
        },
    ) { internalPadding ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(internalPadding)) {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(id = overviewParams.contentIconResId),
                contentDescription = "",
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .padding(
                        horizontal = MaterialTheme.wireDimensions.spacing64x,
                        vertical = MaterialTheme.wireDimensions.spacing32x
                    )
                    .clearAndSetSemantics {}
            )
            OverviewTexts(
                modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing24x),
                onLearnMoreClick = { onLearnMorePressed(overviewParams.learnMoreUrl) },
                overviewParams = overviewParams,
            )
            Spacer(modifier = Modifier.weight(1f))
            WirePrimaryButton(
                text = continueText,
                onClick = onContinuePressed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.wireDimensions.spacing16x),
            )
        }
    }
}

@Composable
private fun OverviewTexts(
    overviewParams: CreateAccountOverviewParams,
    modifier: Modifier = Modifier,
    onLearnMoreClick: () -> Unit
) {
    Column(modifier = modifier) {
        if (overviewParams.contentTitle.isNotEmpty()) {
            Text(
                text = overviewParams.contentTitle,
                style = MaterialTheme.wireTypography.title01,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.wireDimensions.spacing8x)
                    .semantics { heading() }
            )
        }
        Text(
            text = overviewParams.contentText,
            style = MaterialTheme.wireTypography.body02,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {}
        )
        Text(
            text = overviewParams.learnMoreText,
            style = MaterialTheme.wireTypography.body02.copy(
                textDecoration = TextDecoration.Underline
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onLearnMoreClick,
                    onClickLabel = stringResource(CommonR.string.content_description_open_link_label)
                )
        )
    }
}

@Composable
@Preview
fun PreviewCreateAccountOverviewScreen() = WireTheme {
    CreateAccountOverviewContent(
        overviewParams = CreateAccountOverviewParams(
            title = "title",
            contentTitle = "contentTitle",
            contentText = "contentText",
            contentIconResId = R.drawable.ic_create_personal_account,
            learnMoreText = "learn more",
            learnMoreUrl = "",
        ),
        continueText = "Continue",
        backContentDescription = CommonR.string.content_description_left_arrow,
        onBackPressed = {},
        onContinuePressed = {},
        onLearnMorePressed = {},
    )
}
