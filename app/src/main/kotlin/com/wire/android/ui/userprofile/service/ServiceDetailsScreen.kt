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

import com.wire.android.navigation.annotation.app.WireRootDestination
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.ramcosta.composedestinations.generated.app.destinations.ConversationScreenDestination
import com.wire.android.R
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.model.ClickBlockParams
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.avatar.UserProfileAvatarType
import com.wire.android.model.Clickable
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.service.ServiceDetails
import kotlinx.coroutines.launch

@WireRootDestination(
    navArgs = ServiceDetailsNavArgs::class,
    style = PopUpNavigationAnimation::class, // default should be PopUpNavigationAnimation
)
@Composable
fun ServiceDetailsScreen(
    navigator: Navigator,
    viewModel: ServiceDetailsViewModel =
        hiltViewModelScoped<ServiceDetailsViewModelImpl, ServiceDetailsViewModel>()
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    HandleActions(viewModel.actions) { action ->
        when (action) {
            is ServiceDetailsViewActions.Message ->
                coroutineScope.launch {
                    snackbarHostState
                        .showSnackbar(action.message.uiText.asString(context.resources))
                }

            is ServiceDetailsViewActions.OpenConversation ->
                navigator.navigate(
                    NavigationCommand(
                        ConversationScreenDestination(
                            navArgs = ConversationNavArgs(
                                conversationId = action.conversationId
                            )
                        ),
                        BackStackMode.UPDATE_EXISTED
                    )
                )
        }
    }

    ServiceDetailsContent(
        navigateBack = navigator::navigateBack,
        onAddService = viewModel::onAddService,
        onRemoveService = viewModel::onRemoveService,
        onOpenConversation = viewModel::onOpenConversation,
        serviceDetailsState = viewModel.serviceDetailsState()
    )
}

@Composable
private fun ServiceDetailsContent(
    navigateBack: () -> Unit,
    onAddService: () -> Unit,
    onRemoveService: () -> Unit,
    onOpenConversation: () -> Unit,
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
                } ?: ServiceDetailsNotFoundScreen()
            }
        },
        bottomBar = {
            ServiceDetailsButtons(
                serviceDetailsState = serviceDetailsState,
                onAddService = onAddService,
                onRemoveService = onRemoveService,
                onOpenConversation = onOpenConversation
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
private fun ServiceDetailsProfileInfo(state: ServiceDetailsState) {
    state.serviceDetails?.let { serviceDetails ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensions().spacing16x,
                    vertical = dimensions().spacing16x
                )
        ) {
            UserProfileAvatar(
                size = dimensions().avatarDefaultMediumSize,
                avatarData = UserAvatarData(
                    asset = state.serviceAvatarAsset,
                    membership = Membership.Service,
                    nameBasedAvatar = NameBasedAvatar(serviceDetails.name, -1)
                ),
                clickable = remember { Clickable(enabled = false) },
                type = UserProfileAvatarType.WithoutIndicators,
            )

            Spacer(modifier = Modifier.width(dimensions().spacing12x))

            Column(verticalArrangement = Arrangement.spacedBy(dimensions().spacing4x)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
                ) {
                    Text(
                        text = serviceDetails.name,
                        style = MaterialTheme.wireTypography.title02,
                        color = MaterialTheme.colorScheme.onBackground,
                        overflow = TextOverflow.Visible,
                        maxLines = 1,
                    )
                    UserBadge(
                        membership = Membership.Service,
                        connectionState = null
                    )
                }

                serviceDetails.creator?.takeIf { it.isNotBlank() }?.let { creator ->
                    Text(
                        text = stringResource(id = R.string.service_details_created_by, creator),
                        style = MaterialTheme.wireTypography.body01,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                val categoryOrTag = serviceDetails.category ?: serviceDetails.tags.firstOrNull()
                categoryOrTag?.let {
                    Text(
                        text = categoryOrTag.replaceFirstChar { it.uppercaseChar() },
                        style = MaterialTheme.wireTypography.body01,
                        color = colorsScheme().secondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceDetailsDescription(serviceDetails: ServiceDetails) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions().spacing16x)
    ) {
        if (serviceDetails.summary.isNotBlank()) {
            Text(
                text = serviceDetails.summary,
                style = MaterialTheme.wireTypography.body02
            )

            Spacer(modifier = Modifier.height(dimensions().spacing12x))
        }

        Text(
            text = serviceDetails.description,
            style = MaterialTheme.wireTypography.body01,
            color = colorsScheme().secondaryText
        )
    }
}

@Composable
private fun ServiceDetailsAddOrRemoveButton(
    buttonState: ServiceDetailsButtonState,
    onAddService: () -> Unit,
    onRemoveService: () -> Unit
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
                    onClick = if (buttonState == ServiceDetailsButtonState.ADD) onAddService else onRemoveService,
                    clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                    modifier = Modifier
                        .weight(1f)
                        .padding(dimensions().spacing16x)
                )
            }
        }
    }
}

@Composable
private fun ServiceDetailsStartOrOpenConversation(
    isDataLoading: Boolean,
    isConversationStarted: Boolean,
    onOpenConversation: () -> Unit
) {
    if (!isDataLoading) {
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
                    text = stringResource(
                        id = if (isConversationStarted) {
                            R.string.label_open_conversation
                        } else {
                            R.string.label_start_conversation
                        }
                    ),
                    onClick = onOpenConversation,
                    clickBlockParams = ClickBlockParams(
                        blockWhenSyncing = true,
                        blockWhenConnecting = true
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(dimensions().spacing16x)
                )
            }
        }
    }
}

@Composable
private fun ServiceDetailsButtons(
    serviceDetailsState: ServiceDetailsState,
    onAddService: () -> Unit,
    onRemoveService: () -> Unit,
    onOpenConversation: () -> Unit
) {
    when {
        serviceDetailsState.conversationId != null -> ServiceDetailsAddOrRemoveButton(
            buttonState = serviceDetailsState.buttonState,
            onAddService = onAddService,
            onRemoveService = onRemoveService
        )
        serviceDetailsState.isAppsEnabled -> ServiceDetailsStartOrOpenConversation(
            isDataLoading = serviceDetailsState.isDataLoading,
            isConversationStarted = serviceDetailsState.isConversationStarted,
            onOpenConversation = onOpenConversation
        )
    }
}

@Preview
@Composable
fun PreviewServiceDetailsScreen() {
    ServiceDetailsContent(
        navigateBack = {},
        onAddService = {},
        onRemoveService = {},
        onOpenConversation = {},
        serviceDetailsState = ServiceDetailsState()
    )
}
