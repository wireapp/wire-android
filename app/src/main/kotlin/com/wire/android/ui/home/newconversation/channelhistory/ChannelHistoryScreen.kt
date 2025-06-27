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

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.typography
import com.wire.android.ui.destinations.ChannelHistoryCustomAmountScreenDestination
import com.wire.android.ui.home.conversations.details.options.ArrowType
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.newconversation.common.NewConversationNavGraph
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@NewConversationNavGraph
@WireDestination(
    style = SlideNavigationAnimation::class,
)
@Composable
fun ChannelHistoryScreen(
    navigator: WireNavigator,
    cusstomAmountResultRecipient: ResultRecipient<ChannelHistoryCustomAmountScreenDestination, ChannelHistoryCustomAmountNavBackArgs>,
    newConversationViewModel: NewConversationViewModel,
    modifier: Modifier = Modifier,
) {
    cusstomAmountResultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {}
            is NavResult.Value -> newConversationViewModel.setChannelHistoryType(result.value.customType)
        }
    }

    ChannelHistoryScreenContent(
        selectedHistoryOption = newConversationViewModel.newGroupState.channelHistoryType,
        onHistoryOptionSelected = newConversationViewModel::setChannelHistoryType,
        onOpenCustomChooser = {
            val navArgs = ChannelHistoryCustomAmountArgs(newConversationViewModel.newGroupState.channelHistoryType)
            navigator.navigate(NavigationCommand(ChannelHistoryCustomAmountScreenDestination(navArgs)))
        },
        onBackPressed = navigator::navigateBack,
        modifier = modifier,
    )
}

@Composable
fun ChannelHistoryScreenContent(
    selectedHistoryOption: ChannelHistoryType,
    onHistoryOptionSelected: (ChannelHistoryType) -> Unit,
    onOpenCustomChooser: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
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
            val items = defaultHistoryTypes.plus(
                if (selectedHistoryOption.isCustom()) selectedHistoryOption // add the chosen custom option if it is selected
                else ChannelHistoryType.On.Specific(0, ChannelHistoryType.On.Specific.AmountType.Days) // placeholder for custom option
            )

            items(count = items.size) { index ->
                val item = items[index]
                val isSelected = item == selectedHistoryOption
                GroupConversationOptionsItem(
                    title = item.name(useAmountForCustom = false),
                    arrowLabel =
                        if (isSelected && item is ChannelHistoryType.On.Specific && item.isCustom()) item.amountAsString()
                        else null,
                    arrowType = ArrowType.CENTER_ALIGNED,
                    selected = selectedHistoryOption == item,
                    clickable = Clickable {
                        if (item is ChannelHistoryType.On.Specific && item.isCustom()) onOpenCustomChooser()
                        else onHistoryOptionSelected(item)
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
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewChannelHistoryScreen() = WireTheme {
    ChannelHistoryScreenContent(
        selectedHistoryOption = ChannelHistoryType.On.Specific(2, ChannelHistoryType.On.Specific.AmountType.Weeks),
        onHistoryOptionSelected = {},
        onOpenCustomChooser = {},
        onBackPressed = {},
    )
}
