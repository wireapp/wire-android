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
package com.wire.android.ui.userprofile.service
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireRootNavGraph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.common.EditableState
import com.wire.android.ui.userprofile.common.UserProfileInfo
import com.wire.kalium.logic.data.service.ServiceDetails

@Destination<WireRootNavGraph>(
    navArgs = ServiceDetailsNavArgs::class,
    style = DestinationStyle.Runtime::class, // default should be PopUpNavigationAnimation
)
@Composable
fun ServiceDetailsScreen(
    navigator: Navigator,
    viewModel: ServiceDetailsViewModel = hiltViewModel()
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.infoMessage.collect {
            snackbarHostState.showSnackbar(it.asString(context.resources))
        }
    }

    ServiceDetailsContent(
        navigateBack = navigator::navigateBack,
        addService = viewModel::addService,
        removeService = viewModel::removeService,
        serviceDetailsState = viewModel.serviceDetailsState
    )
}

@Composable
private fun ServiceDetailsContent(
    navigateBack: () -> Unit,
    addService: () -> Unit,
    removeService: () -> Unit,
    serviceDetailsState: ServiceDetailsState
) {
    WireScaffold(
        topBar = {
            Column {
                ServiceDetailsTopAppBar(
                    onBackPressed = navigateBack
                )
            }
        },
        content = { internalPadding ->
            Column(modifier = Modifier.padding(internalPadding)) {
                serviceDetailsState.serviceDetails?.let { serviceDetails ->
                    ServiceDetailsProfileInfo(state = serviceDetailsState)
                    ServiceDetailsDescription(serviceDetails = serviceDetails)
                } ?: ServiceDetailsNotFoundScreen(
                    modifier = Modifier.padding(bottom = dimensions().spacing16x)
                )
            }
        },
        bottomBar = {
            ServiceDetailsAddOrRemoveButton(
                buttonState = serviceDetailsState.buttonState,
                addService = addService,
                removeService = removeService
            )
        }
    )
}

@Composable
private fun ServiceDetailsTopAppBar(
    onBackPressed: () -> Unit,
) {
    WireCenterAlignedTopAppBar(
        elevation = dimensions().spacing0x,
        title = stringResource(id = R.string.service_details_label),
        onNavigationPressed = onBackPressed
    )
}

@Composable
private fun ServiceDetailsProfileInfo(
    state: ServiceDetailsState
) {
    state.serviceDetails?.let { serviceDetails ->
        UserProfileInfo(
            userId = state.serviceMemberId,
            isLoading = state.isAvatarLoading,
            avatarAsset = state.serviceAvatarAsset,
            fullName = serviceDetails.name,
            userName = "",
            teamName = null,
            membership = Membership.Service,
            editableState = EditableState.NotEditable,
            modifier = Modifier.padding(bottom = dimensions().spacing16x)
        )
    }
}

@Composable
private fun ServiceDetailsDescription(
    serviceDetails: ServiceDetails
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.Center)
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.spacing16x))
        Text(
            text = serviceDetails.description,
            style = MaterialTheme.wireTypography.body01
        )
        Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.spacing24x))
        Text(
            text = serviceDetails.summary,
            style = MaterialTheme.wireTypography.body01,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = dimensions().spacing18x)
        )
    }
}

@Composable
private fun ServiceDetailsAddOrRemoveButton(
    buttonState: ServiceDetailsButtonState,
    addService: () -> Unit,
    removeService: () -> Unit
) {
    val (shouldShow: Boolean, textString: String?) = when (buttonState) {
        ServiceDetailsButtonState.HIDDEN -> Pair(false, null)
        ServiceDetailsButtonState.ADD -> Pair(true, stringResource(id = R.string.service_details_add_service_label))
        ServiceDetailsButtonState.REMOVE -> Pair(true, stringResource(id = R.string.service_details_remove_service_label))
    }
    if (shouldShow) {
        Surface(
            color = MaterialTheme.wireColorScheme.background,
            shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation
        ) {
            HorizontalDivider(color = colorsScheme().outline)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WirePrimaryButton(
                    text = textString,
                    onClick = if (buttonState == ServiceDetailsButtonState.ADD) addService else removeService,
                    clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                    modifier = Modifier
                        .weight(1f)
                        .padding(dimensions().spacing16x)
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewServiceDetailsScreen() {
    ServiceDetailsContent({}, {}, {}, ServiceDetailsState())
}
