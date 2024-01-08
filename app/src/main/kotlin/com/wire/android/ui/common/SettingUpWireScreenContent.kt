/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.wire.android.ui.common.scaffold.WireScaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun SettingUpWireScreenContent(
    @StringRes topBarTitleResId: Int = R.string.migration_title,
    @DrawableRes iconResId: Int = R.drawable.ic_migration,
    title: String? = null,
    message: AnnotatedString = AnnotatedString(stringResource(R.string.migration_message)),
    type: SettingUpWireScreenType = SettingUpWireScreenType.Progress
) {
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = stringResource(topBarTitleResId),
                navigationIconType = null,
            )
        }
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = true)
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
                AnimatedVisibility(visible = type is SettingUpWireScreenType.Progress) {
                    WireCircularProgressIndicator(progressColor = MaterialTheme.wireColorScheme.badge)
                }
                AnimatedVisibility(visible = title?.isNotEmpty() == true) {
                    Text(
                        text = title ?: "",
                        style = MaterialTheme.wireTypography.body02,
                        color = MaterialTheme.wireColorScheme.onBackground,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .animateContentSize()
                            .padding(horizontal = MaterialTheme.wireDimensions.welcomeTextHorizontalPadding)
                    )
                }

                Text(
                    text = message,
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .animateContentSize()
                        .padding(horizontal = MaterialTheme.wireDimensions.welcomeTextHorizontalPadding)
                )
            }

            Box(modifier = Modifier.animateContentSize()) {
                if (type is SettingUpWireScreenType.Failure) {
                    Surface(
                        shadowElevation = MaterialTheme.wireDimensions.topBarShadowElevation,
                        color = MaterialTheme.wireColorScheme.background,
                    ) {
                        Box(modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
                            WirePrimaryButton(
                                onClick = (type as? SettingUpWireScreenType.Failure)?.onButtonClick ?: {},
                                text = (type as? SettingUpWireScreenType.Failure)?.buttonTextResId?.let { stringResource(it) },
                                fillMaxWidth = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class SettingUpWireScreenType {
    object Progress : SettingUpWireScreenType()
    data class Failure(
        @StringRes val buttonTextResId: Int = R.string.label_retry,
        val onButtonClick: () -> Unit = {}
    ) : SettingUpWireScreenType()
}

@Preview
@Composable
fun PreviewMigrationScreenProgress() {
    SettingUpWireScreenContent(type = SettingUpWireScreenType.Progress, title = "Step 1/2")
}

@Preview
@Composable
fun PreviewMigrationScreenFailure() {
    SettingUpWireScreenContent(type = SettingUpWireScreenType.Failure())
}
