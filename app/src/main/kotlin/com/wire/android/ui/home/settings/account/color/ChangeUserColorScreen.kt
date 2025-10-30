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

package com.wire.android.ui.home.settings.account.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.WireDropDown
import com.wire.android.ui.common.button.WireButtonState.Default
import com.wire.android.ui.common.button.WireButtonState.Disabled
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.messages.item.MessageClickActions
import com.wire.android.ui.home.conversations.messages.item.RegularMessageItem
import com.wire.android.ui.home.conversations.mock.mockEmptyFooter
import com.wire.android.ui.home.conversations.mock.mockHeader
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.conversations.mock.mockMessageWithTextContent
import com.wire.android.ui.home.conversations.model.MessageFooter
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.resourceId
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.QualifiedID

@WireDestination(
    style = DestinationStyle.Runtime::class, // default should be SlideNavigationAnimation
)
@Composable
fun ChangeUserColorScreen(
    navigator: Navigator,
    resultNavigator: ResultBackNavigator<Boolean>,
    viewModel: ChangeUserColorViewModel = hiltViewModel()
) {
    with(viewModel) {
        ChangeUserColorContent(
            state = viewModel.accentState,
            onSavePressed = ::saveAccentColor,
            onChangePressed = ::changeAccentColor,
            onBackPressed = navigator::navigateBack
        )

        HandleActions(actions) {
            when (it) {
                ChangeUserColorAction.Success -> {
                    resultNavigator.setResult(true)
                    resultNavigator.navigateBack()
                }

                ChangeUserColorAction.Failure -> {
                    resultNavigator.setResult(false)
                    resultNavigator.navigateBack()
                }
            }
        }
    }
}

@Composable
fun ChangeUserColorContent(
    state: AccentActionState,
    onChangePressed: (Accent) -> Unit,
    onSavePressed: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    with(state) {
        WireScaffold(
            modifier = modifier,
            topBar = {
                WireCenterAlignedTopAppBar(
                    elevation = scrollState.rememberTopBarElevationState().value,
                    onNavigationPressed = onBackPressed,
                    title = stringResource(id = R.string.settings_myaccount_user_color_title)
                )
            },
            bottomBar = {
                Box(modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
                    WirePrimaryButton(
                        text = stringResource(R.string.label_save),
                        onClick = onSavePressed,
                        fillMaxWidth = true,
                        trailingIcon = Icons.Filled.ChevronRight.Icon(),
                        state = if (state.accent != null) Default else Disabled,
                        loading = state.isPerformingAction,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        ) { internalPadding ->
            Column(
                modifier = Modifier
                    .padding(internalPadding)
                    .fillMaxSize()
            ) {
                Text(
                    text = stringResource(id = R.string.settings_myaccount_user_color_description),
                    style = MaterialTheme.wireTypography.body01,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.wireDimensions.spacing16x,
                            vertical = MaterialTheme.wireDimensions.spacing16x
                        )
                )

                VerticalSpace.x24()

                state.accent?.let { accentColor ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(stringResource(R.string.settings_myaccount_user_color))

                        val items = Accent.entries.filter { accent -> accent != Accent.Unknown }

                        WireDropDown(
                            items = items.map { stringResource(it.resourceId()) },
                            defaultItemIndex = if(accentColor == Accent.Unknown) -1 else items.indexOf(accentColor),
                            label = null,
                            modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing16x),
                            autoUpdateSelection = false,
                            showDefaultTextIndicator = false,
                            leadingCompose = { index -> if (index >= 0) AccentCircle(Accent.entries[index]) else null },
                            onChangeClickDescription = stringResource(R.string.content_description_settings_myaccount_user_color)
                        ) { selectedIndex ->
                            onChangePressed(items[selectedIndex])
                        }
                    }
                }

                VerticalSpace.x24()
                if (state.isMessageBubbleEnabled) {
                    SectionHeader(stringResource(R.string.settings_myaccount_user_color_example))
                    VerticalSpace.x4()

                    Column(
                        modifier = Modifier
                            .weight(weight = 1f, fill = true)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                    ) {

                        Column(modifier = Modifier.background(colorsScheme().surface)) {

                            VerticalSpace.x12()

                            RegularMessageItem(
                                message = mockMessageWithTextContent(SELF_USER_MESSAGE_TEXT).copy(
                                    header = mockMessageWithText.header.copy(
                                        username = UIText.DynamicString(
                                            "Paul Nagel"
                                        ),
                                        accent = state.accent ?: Accent.Blue
                                    ),
                                    messageFooter = mockEmptyFooter,
                                ),
                                conversationDetailsData = ConversationDetailsData.Group(null, QualifiedID("value", "domain")),
                                clickActions = MessageClickActions.Content(),
                                isBubbleUiEnabled = true,
                            )

                            RegularMessageItem(
                                message = mockMessageWithTextContent("Have a look here: www.wire.com").copy(
                                    source = MessageSource.OtherUser,
                                    header = mockHeader.copy(
                                        username = UIText.DynamicString(
                                            "Paul Nagel"
                                        ),
                                        membership = Membership.Standard
                                    ),
                                    messageFooter = MessageFooter(
                                        messageId = "messageId",
                                        reactions = mapOf(
                                            "👍" to 16,
                                            "❤️" to 12,
                                        ),
                                        ownReactions = setOf("👍"),
                                    ),
                                ),
                                conversationDetailsData = ConversationDetailsData.Group(null, QualifiedID("value", "domain")),
                                clickActions = MessageClickActions.Content(),
                                isBubbleUiEnabled = true,
                            )

                            VerticalSpace.x24()
                        }
                    }
                }
            }
        }
    }
}

private const val SELF_USER_MESSAGE_TEXT = "Hi team, any news on the pitch deck? " +
        "We have a meeting next week and we should specify some of the details until then."

@Composable
fun AccentCircle(accent: Accent, modifier: Modifier = Modifier) {
    if (accent == Accent.Unknown) return
    val color = MaterialTheme.wireColorScheme.wireAccentColors.getOrDefault(
        accent,
        MaterialTheme.wireColorScheme.primary
    )
    Canvas(modifier = modifier.size(dimensions().spacing16x)) {
        drawCircle(color = color)
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewChangeUserColor() = WireTheme {
    ChangeUserColorContent(AccentActionState(Accent.Blue), {}, {}, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewChangeUserColorWithExample() = WireTheme {
    ChangeUserColorContent(AccentActionState(Accent.Blue, isMessageBubbleEnabled = true), {}, {}, {})
}
