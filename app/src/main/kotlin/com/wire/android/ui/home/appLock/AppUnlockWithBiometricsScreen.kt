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
 */
package com.wire.android.ui.home.appLock

import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.biomitric.showBiometricPrompt
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.destinations.EnterLockCodeScreenDestination

@RootNavGraph
@Destination
@Composable
fun AppUnlockWithBiometricsScreen(
    appUnlockWithBiometricsViewModel: AppUnlockWithBiometricsViewModel = hiltViewModel(),
    navigator: Navigator,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorsScheme().background)
    ) {
        Icon(
            modifier = Modifier
                .padding(top = dimensions().spacing80x)
                .align(Alignment.TopCenter),
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_wire_logo),
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = stringResource(id = R.string.content_description_welcome_wire_logo)
        )

        val activity = LocalContext.current
        LaunchedEffect(Unit) {
            (activity as AppCompatActivity).showBiometricPrompt(
                onSuccess = {
                    appUnlockWithBiometricsViewModel.onAppUnlocked()
                    navigator.navigateBack()
                },
                onCancel = {
                    navigator.finish()
                },
                onRequestPasscode = {
                    navigator.navigate(
                        NavigationCommand(
                            EnterLockCodeScreenDestination(),
                            BackStackMode.CLEAR_WHOLE
                        )
                    )
                }
            )
        }
    }
    BackHandler {
        navigator.finish()
    }
}
