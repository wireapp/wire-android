/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.cells.ui.createfolder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.feature.cells.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.navigation.PreviewNavigator
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import java.util.Locale

@WireDestination(
    style = PopUpNavigationAnimation::class
)
@Composable
fun CreateFolderScreen(
    navigator: WireNavigator,
    modifier: Modifier = Modifier,
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = { navigator.navigateBack() },
                navigationIconType = NavigationIconType.Close(),
                elevation = dimensions().spacing0x,
                title = stringResource(id = R.string.cells_create_folder),
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Surface(
                    color = MaterialTheme.wireColorScheme.background,
                    shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(dimensions().spacing16x)
                    ) {
                        WirePrimaryButton(
                            text = stringResource(R.string.cells_create_folder),
                            onClick = {},
                            state = WireButtonState.Default,
                            clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                        )
                    }
                }
            }
        }
    ) {
        WireTextField(
            textState = TextFieldState(),
            placeholderText = stringResource(R.string.cells_folder_name),
            labelText = stringResource(R.string.cells_folder_name).uppercase(Locale.getDefault()),
            modifier = Modifier
                .padding(it)
                .padding(
                    top = dimensions().spacing32x,
                    start = dimensions().spacing16x,
                    end = dimensions().spacing16x
                )
        )
    }
}

@MultipleThemePreviews
@Composable
fun PreviewCreateFolderScreen() {
    WireTheme {
        CreateFolderScreen(navigator = PreviewNavigator)
    }
}
