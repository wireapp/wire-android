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
package com.wire.android.ui.e2eiEnrollment

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.WireDialogContent
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.destinations.E2EIEnrollmentScreenDestination
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.InitialSyncScreenDestination

@RootNavGraph
@Destination(navArgsDelegate = E2EIEnrollmentNavArgs::class)
@Composable
fun E2EIEnrollmentScreen(
    navigator: Navigator,
    viewModel: E2EIEnrollmentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    WireDialogContent(
        title = "e2ei is required",
        optionButton1Properties = WireDialogButtonProperties(
            text = "Enroll",
            onClick = { viewModel.enrollE2EICertificate(context) },
            type = WireDialogButtonType.Primary
        )
    )
//    if (viewModel.state.certificate.isNotEmpty() && viewModel.state.certificate.isNotBlank()) {
//        navigator.navigate(
//            NavigationCommand(
//                InitialSyncScreenDestination,
//                BackStackMode.CLEAR_WHOLE
//            )
//        )
//    }

//    LaunchedEffect(Unit) {
//        delay(AnimationConstants.DefaultDurationMillis.toLong()) // it can be triggered instantly so it's added to keep smooth transitions
//        viewModel.waitUntilSyncIsCompleted {
//            navigator.navigate(NavigationCommand(HomeScreenDestination, BackStackMode.CLEAR_WHOLE))
//        }
//    }
}


