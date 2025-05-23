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

package com.wire.android.ui.authentication.create.summary

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.wire.android.ui.common.scaffold.WireScaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.common.CreatePersonalAccountNavGraph
import com.wire.android.ui.authentication.create.common.CreateTeamAccountNavGraph
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.CreateAccountUsernameScreenDestination
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@CreatePersonalAccountNavGraph
@CreateTeamAccountNavGraph
@WireDestination(navArgsDelegate = CreateAccountSummaryNavArgs::class)
@Composable
fun CreateAccountSummaryScreen(
    navigator: Navigator,
    viewModel: CreateAccountSummaryViewModel = hiltViewModel()
) {
    SummaryContent(
        state = viewModel.summaryState,
        onContinuePressed = { navigator.navigate(NavigationCommand(CreateAccountUsernameScreenDestination, BackStackMode.CLEAR_WHOLE)) }
    )
}

@Composable
private fun SummaryContent(
    state: CreateAccountSummaryViewState,
    onContinuePressed: () -> Unit
) {
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = dimensions().spacing0x,
                title = stringResource(id = state.type.summaryResources.summaryTitleResId),
                navigationIconType = null
            )
        },
    ) { internalPadding ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(internalPadding)) {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(id = state.type.summaryResources.summaryIconResId),
                contentDescription = null,
                contentScale = ContentScale.Inside,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.wireDimensions.spacing64x,
                    vertical = MaterialTheme.wireDimensions.spacing32x
                )
            )
            Text(
                text = stringResource(id = state.type.summaryResources.summaryTextResId),
                style = MaterialTheme.wireTypography.body02,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.wireDimensions.spacing24x)
            )
            Spacer(modifier = Modifier.weight(1f))
            WirePrimaryButton(
                text = stringResource(R.string.label_get_started),
                onClick = onContinuePressed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.wireDimensions.spacing16x)
            )
        }
    }
}

@Preview
@Composable
fun PreviewCreateAccountSummaryScreen() {
    SummaryContent(CreateAccountSummaryViewState(CreateAccountFlowType.CreatePersonalAccount), {})
}
