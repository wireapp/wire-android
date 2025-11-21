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
package com.wire.android.ui.home.newconversation.channelhistory
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireRootNavGraph

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.ui.destinations.ChannelHistoryCustomScreenDestination
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.ui.common.WirePromotionCard
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.details.options.ArrowType
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.newconversation.common.NewConversationNavGraph
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@NewConversationNavGraph
@Destination<WireRootNavGraph>(
    style = SlideNavigationAnimation::class,
)
@Composable
fun ChannelHistoryScreen(
    navigator: WireNavigator,
    customResultRecipient: ResultRecipient<ChannelHistoryCustomScreenDestination, ChannelHistoryCustomNavBackArgs>,
    newConversationViewModel: NewConversationViewModel,
    modifier: Modifier = Modifier,
) {
    customResultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {}
            is NavResult.Value -> newConversationViewModel.setChannelHistoryType(result.value.customType)
        }
    }

    ChannelHistoryScreenContent(
        selectedHistoryOption = newConversationViewModel.newGroupState.channelHistoryType,
        onHistoryOptionSelected = newConversationViewModel::setChannelHistoryType,
        onOpenCustomChooser = {
            val navArgs = ChannelHistoryCustomArgs(newConversationViewModel.newGroupState.channelHistoryType)
            navigator.navigate(NavigationCommand(ChannelHistoryCustomScreenDestination(navArgs)))
        },
        onBackPressed = navigator::navigateBack,
        onUpgradeNowClicked = { /* TODO: Implement upgrade action */ },
        modifier = modifier,
    )
}

@Suppress("CyclomaticComplexMethod")
@Composable
fun ChannelHistoryScreenContent(
    selectedHistoryOption: ChannelHistoryType,
    onHistoryOptionSelected: (ChannelHistoryType) -> Unit,
    onOpenCustomChooser: () -> Unit,
    onBackPressed: () -> Unit,
    onUpgradeNowClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isFreemiumAccount: Boolean = false
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onBackPressed,
                elevation = dimensions().spacing0x,
                title = stringResource(id = R.string.channel_history_label),
                titleContentDescription = stringResource(id = R.string.content_description_new_conversation_history_heading),
                navigationIconType = NavigationIconType.Back(R.string.content_description_new_group_options_back_btn)
            )
        }
    ) { internalPadding ->
        LazyColumn(modifier = Modifier.padding(internalPadding)) {
            val items = when (isFreemiumAccount) {
                true -> defaultFreemiumHistoryTypes
                false -> defaultHistoryTypes.plus(
                    when {
                        // add the chosen custom option if it is selected
                        selectedHistoryOption.isCustom() -> selectedHistoryOption
                        // otherwise add a placeholder for custom option
                        else -> ChannelHistoryType.On.Specific(0, ChannelHistoryType.On.Specific.AmountType.Days)
                    }
                )
            }

            items(count = items.size) { index ->
                val item = items[index]
                val isSelected = item == selectedHistoryOption
                GroupConversationOptionsItem(
                    title = item.name(useAmountForCustom = false),
                    arrowLabel = when {
                        isSelected && item is ChannelHistoryType.On.Specific && item.isCustom() -> item.amountAsString()
                        else -> null
                    },
                    arrowType = when {
                        item.isCustom() -> ArrowType.CENTER_ALIGNED
                        else -> ArrowType.NONE
                    },
                    selected = selectedHistoryOption == item,
                    clickable = Clickable {
                        when {
                            item is ChannelHistoryType.On.Specific && item.isCustom() -> onOpenCustomChooser()
                            else -> onHistoryOptionSelected(item)
                        }
                    },
                )
            }
            item {
                Text( // footer description text
                    text = stringResource(id = R.string.channel_history_description),
                    style = typography().body01,
                    color = colorsScheme().secondaryText,
                    modifier = Modifier.padding(
                        top = dimensions().spacing4x,
                        bottom = dimensions().spacing16x,
                        start = dimensions().spacing16x,
                        end = dimensions().spacing16x,
                    )
                )
            }
            if (isFreemiumAccount) {
                item {
                    ChannelHistoryFreemiumUpgradeCard(
                        onUpgradeNowClicked = onUpgradeNowClicked,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelHistoryFreemiumUpgradeCard(
    onUpgradeNowClicked: () -> Unit,
) {
    WirePromotionCard(
        title = stringResource(id = R.string.channel_history_freemium_upgrade_title),
        description = stringResource(id = R.string.channel_history_freemium_upgrade_description),
        buttonLabel = stringResource(id = R.string.channel_history_freemium_upgrade_now),
        onButtonClick = onUpgradeNowClicked,
        modifier = Modifier.padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x),
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewChannelHistoryScreenPremium() = WireTheme {
    ChannelHistoryScreenContent(
        isFreemiumAccount = false,
        selectedHistoryOption = ChannelHistoryType.On.Specific(2, ChannelHistoryType.On.Specific.AmountType.Weeks),
        onHistoryOptionSelected = {},
        onOpenCustomChooser = {},
        onUpgradeNowClicked = {},
        onBackPressed = {},
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewChannelHistoryScreenFreemium() = WireTheme {
    ChannelHistoryScreenContent(
        isFreemiumAccount = true,
        selectedHistoryOption = ChannelHistoryType.On.Specific(1, ChannelHistoryType.On.Specific.AmountType.Days),
        onHistoryOptionSelected = {},
        onOpenCustomChooser = {},
        onUpgradeNowClicked = {},
        onBackPressed = {},
    )
}
