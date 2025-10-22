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

package com.wire.android.ui.home.newconversation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun CreateRegularGroupOrChannelButtons(
    shouldShowChannelPromotion: Boolean,
    isUserAllowedToCreateChannels: Boolean,
    onCreateNewRegularGroup: () -> Unit,
    onCreateNewChannel: () -> Unit,
    modifier: Modifier = Modifier,
    elevation: Dp = MaterialTheme.wireDimensions.bottomNavigationShadowElevation,
) {
    Surface(
        color = MaterialTheme.wireColorScheme.background,
        shadowElevation = elevation
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = modifier
                .padding(dimensions().spacing16x)
        ) {
            if (isUserAllowedToCreateChannels || shouldShowChannelPromotion) {
                WirePrimaryButton(
                    text = stringResource(R.string.label_create_new_channel),
                    onClick = onCreateNewChannel,
                    state = WireButtonState.Default,
                    leadingIcon = {
                        Image(
                            painter = painterResource(id = com.wire.android.ui.common.R.drawable.ic_channel),
                            contentDescription = null,
                            modifier = Modifier.padding(end = dimensions().spacing12x),
                            colorFilter = ColorFilter.tint(colorsScheme().onPrimaryButtonEnabled)
                        )
                    },
                    trailingIcon = {
                        if (shouldShowChannelPromotion) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_upgrade),
                                contentDescription = null,
                                modifier = Modifier.padding(end = dimensions().spacing12x),
                            )
                        }
                    },
                    clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                    modifier = Modifier.padding(bottom = dimensions().spacing12x)
                )
            }

            WirePrimaryButton(
                text = stringResource(R.string.label_create_new_group),
                onClick = onCreateNewRegularGroup,
                state = WireButtonState.Default,
                leadingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_group),
                        contentDescription = null,
                        modifier = Modifier.padding(end = dimensions().spacing12x),
                        colorFilter = ColorFilter.tint(colorsScheme().onPrimaryButtonEnabled)
                    )
                },
                clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewCreateRegularGroupOrChannelButtonsWithPromotion() {
    WireTheme {
        CreateRegularGroupOrChannelButtons(
            shouldShowChannelPromotion = true,
            isUserAllowedToCreateChannels = true,
            onCreateNewRegularGroup = { },
            onCreateNewChannel = { }
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewCreateRegularGroupOrChannelButtons() {
    WireTheme {
        CreateRegularGroupOrChannelButtons(
            shouldShowChannelPromotion = false,
            isUserAllowedToCreateChannels = true,
            onCreateNewRegularGroup = { },
            onCreateNewChannel = { }
        )
    }
}
