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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.feature.e2ei.OAuthUseCase
import com.wire.android.util.extension.getActivity
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.feature.e2ei.usecase.E2EIEnrollmentResult
import com.wire.kalium.common.functional.Either
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun GetE2EICertificateUI(
    enrollmentResultHandler: (Either<CoreFailure, E2EIEnrollmentResult>) -> Unit,
    isNewClient: Boolean,
    viewModel: GetE2EICertificateViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.requestOAuthFlow.onEach {
            OAuthUseCase(context, it.target, it.oAuthClaims).launch(
                context.getActivity()!!.activityResultRegistry, forceLoginFlow = true
            ) { result -> viewModel.handleOAuthResult(result, it) }
        }.launchIn(coroutineScope)
    }

    LaunchedEffect(Unit) {
        viewModel.enrollmentResultFlow.onEach { enrollmentResultHandler(it) }.launchIn(coroutineScope)
    }
    LaunchedEffect(Unit) {
        viewModel.getCertificate(isNewClient)
    }
}
