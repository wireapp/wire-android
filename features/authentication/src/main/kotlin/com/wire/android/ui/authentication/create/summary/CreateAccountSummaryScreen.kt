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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.feature.authentication.R
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun CreateAccountSummaryRouteScreen(
    type: CreateAccountRouteFlowType,
    onContinue: () -> Unit,
) {
    SummaryContent(
        type = type,
        onContinuePressed = onContinue,
    )
}

@Composable
private fun SummaryContent(
    type: CreateAccountRouteFlowType,
    onContinuePressed: () -> Unit
) {
    val resources = type.summaryResources()
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = dimensions().spacing0x,
                title = stringResource(id = resources.title),
                navigationIconType = null
            )
        },
    ) { internalPadding ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(internalPadding)) {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(id = resources.icon),
                contentDescription = null,
                contentScale = ContentScale.Inside,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.wireDimensions.spacing64x,
                    vertical = MaterialTheme.wireDimensions.spacing32x
                )
            )
            Text(
                text = stringResource(id = resources.text),
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

@MultipleThemePreviews
@Composable
internal fun PreviewCreateAccountSummaryScreen() = WireTheme {
    SummaryContent(CreateAccountRouteFlowType.PERSONAL, {})
}

internal fun CreateAccountRouteFlowType.summaryResources(): CreateAccountSummaryResources = when (this) {
    CreateAccountRouteFlowType.PERSONAL -> CreateAccountSummaryResources(
        title = R.string.create_personal_account_summary_title,
        text = R.string.create_personal_account_summary_text,
        icon = R.drawable.ic_create_personal_account_success,
    )

    CreateAccountRouteFlowType.TEAM -> CreateAccountSummaryResources(
        title = R.string.create_team_summary_title,
        text = R.string.create_team_summary_text,
        icon = R.drawable.ic_create_team_success,
    )
}

internal data class CreateAccountSummaryResources(
    @StringRes val title: Int,
    @StringRes val text: Int,
    @DrawableRes val icon: Int,
)
