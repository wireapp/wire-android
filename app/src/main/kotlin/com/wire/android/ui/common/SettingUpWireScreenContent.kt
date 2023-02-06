/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

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
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.button.WirePrimaryButton
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
    type: SettingUpWireScreenType = SettingUpWireScreenType.Progress
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
            if (type is SettingUpWireScreenType.Progress) {
                WireCircularProgressIndicator(progressColor = MaterialTheme.wireColorScheme.badge)
            }
            Text(
                text = stringResource(messageResId),
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.welcomeTextHorizontalPadding)
            )
            if (type is SettingUpWireScreenType.Failure) {
                WirePrimaryButton(
                    onClick = type.onRetryClick,
                    text = stringResource(type.retryButtonResId),
                    fillMaxWidth = false,
                    modifier = Modifier
                        .padding(
                            horizontal = MaterialTheme.wireDimensions.welcomeButtonHorizontalPadding,
                            vertical = MaterialTheme.wireDimensions.welcomeButtonVerticalPadding
                        )
                )
            }
        }
    }
}

sealed class SettingUpWireScreenType {
    object Progress : SettingUpWireScreenType()
    data class Failure(
        @StringRes val retryButtonResId: Int = R.string.label_retry,
        val onRetryClick: () -> Unit = {}
    ) : SettingUpWireScreenType()
}

@Preview
@Composable
fun PreviewMigrationScreenProgress() {
    SettingUpWireScreenContent(type = SettingUpWireScreenType.Progress)
}

@Preview
@Composable
fun PreviewMigrationScreenFailure() {
    SettingUpWireScreenContent(type = SettingUpWireScreenType.Failure())
}
