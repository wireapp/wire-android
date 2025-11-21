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
package com.wire.android.feature.cells.ui.publiclink.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.feature.cells.R
import com.wire.android.navigation.annotation.features.cells.WireDestination
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar

@WireDestination
@Composable
internal fun PublicLinkPasswordScreen(
    resultNavigator: ResultBackNavigator<Boolean>,
    modifier: Modifier = Modifier,
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = { resultNavigator.navigateBack() },
                title = stringResource(R.string.public_link_setting_password_title),
                navigationIconType = NavigationIconType.Back(),
                elevation = dimensions().spacing0x
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(dimensions().spacing16x)
        ) {

            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
            ) {
                // Screen placeholder UI
            }

            WirePrimaryButton(
                text = stringResource(R.string.save_label),
                onClick = {
                    resultNavigator.navigateBack(true)
                }
            )
        }
    }
}
