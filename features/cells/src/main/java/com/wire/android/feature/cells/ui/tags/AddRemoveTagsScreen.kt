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
package com.wire.android.feature.cells.ui.tags

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.feature.cells.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.chip.WireFilterChip
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@WireDestination(
    navArgsDelegate = AddRemoveTagsNavArgs::class,
    style = PopUpNavigationAnimation::class,
)
@Composable
fun AddRemoveTagsScreen(
    navigator: WireNavigator,
    modifier: Modifier = Modifier,
    addRemoveTagsViewModel: AddRemoveTagsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    WireScaffold(
        modifier = modifier,
        snackbarHost = {},
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.add_or_remove_tags_title),
                navigationIconType = NavigationIconType.Close(),
                onNavigationPressed = {
                    navigator.navigateBack()
                },
            )
        },
        bottomBar = {
            val isLoading = addRemoveTagsViewModel.isLoading.collectAsState().value
            Surface(
                color = MaterialTheme.wireColorScheme.background,
                shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = dimensions().spacing16x)
                        .height(dimensions().groupButtonHeight)
                ) {
                    WirePrimaryButton(
                        text = stringResource(R.string.save_label),
                        onClick = {
                            addRemoveTagsViewModel.updateTags()
                        },
                        state = if (isLoading) WireButtonState.Disabled else WireButtonState.Default,
                        loading = isLoading,
                        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                    )
                }
            }
        }
    ) { internalPadding ->

        AddRemoveTagsScreenContent(
            internalPadding = internalPadding,
            textFieldState = addRemoveTagsViewModel.tagsTextState,
            addedTags = addRemoveTagsViewModel.addedTags.collectAsState().value,
            suggestedTags = addRemoveTagsViewModel.suggestedTags.collectAsState().value,
            onAddTag = { tag ->
                addRemoveTagsViewModel.addTag(tag)
            },
            onRemoveTag = { tag ->
                addRemoveTagsViewModel.removeTag(tag)
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    HandleActions(addRemoveTagsViewModel.actions) { action ->
        when (action) {
            is AddRemoveTagsViewModelAction.Success -> {
                Toast.makeText(context, context.resources.getString(R.string.tags_edited), Toast.LENGTH_SHORT).show()
            }

            is AddRemoveTagsViewModelAction.Failure -> {
                Toast.makeText(context, context.resources.getString(R.string.failed_edit_tags), Toast.LENGTH_SHORT).show()
            }
        }
        navigator.navigateBack()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddRemoveTagsScreenContent(
    internalPadding: PaddingValues,
    textFieldState: TextFieldState,
    addedTags: Set<String>,
    suggestedTags: Set<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(internalPadding)
    ) {
        Text(
            modifier = Modifier.padding(dimensions().spacing16x),
            text = stringResource(R.string.tags_description),
            style = MaterialTheme.wireTypography.body01,
        )

        WireTextField(
            textState = textFieldState,
            placeholderText = stringResource(R.string.enter_name_label),
            inputMinHeight = dimensions().spacing56x,
            shape = CutCornerShape(0.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            trailingIcon = {
                WirePrimaryButton(
                    text = stringResource(R.string.add_tag_label),
                    modifier = Modifier
                        .height(dimensions().spacing32x)
                        .width(dimensions().spacing72x)
                        .padding(end = dimensions().spacing8x),
                    onClick = {
                        onAddTag(textFieldState.text.toString())
                    },
                    state = if (textFieldState.text.isEmpty()) {
                        WireButtonState.Disabled
                    } else {
                        WireButtonState.Default
                    },
                )
            },
        )

        Text(
            modifier = Modifier.padding(
                top = dimensions().spacing24x,
                start = dimensions().spacing16x,
                end = dimensions().spacing16x,
                bottom = dimensions().spacing10x,
            ),
            text = stringResource(R.string.added_tags_label).uppercase(),
            style = MaterialTheme.wireTypography.label01.copy(
                color = colorsScheme().secondaryText,
            )
        )
        AnimatedContent(addedTags.isEmpty()) { isEmpty ->
            if (isEmpty) {
                Text(
                    modifier = Modifier.padding(
                        start = dimensions().spacing16x,
                        end = dimensions().spacing16x,
                        bottom = dimensions().spacing16x,
                        top = dimensions().spacing10x
                    ),
                    text = stringResource(R.string.no_tags_message),
                    style = MaterialTheme.wireTypography.body01
                )
            } else {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorsScheme().surfaceVariant)
                        .padding(
                            top = dimensions().spacing16x,
                            start = dimensions().spacing16x,
                            end = dimensions().spacing16x,
                            bottom = dimensions().spacing8x
                        )
                        .animateContentSize()
                ) {
                    addedTags.forEach { tag ->
                        WireFilterChip(
                            modifier = Modifier.padding(
                                end = dimensions().spacing8x,
                                bottom = dimensions().spacing8x
                            ),
                            label = tag,
                            isSelected = true,
                            onSelectChip = onRemoveTag
                        )
                    }
                }
            }
        }

        Text(
            modifier = Modifier.padding(
                top = dimensions().spacing28x,
                bottom = dimensions().spacing10x,
                start = dimensions().spacing16x,
                end = dimensions().spacing16x
            ),
            text = stringResource(R.string.suggested_tags_label).uppercase(),
            style = MaterialTheme.wireTypography.label01.copy(
                color = colorsScheme().secondaryText,
            )
        )

        HorizontalDivider(color = MaterialTheme.wireColorScheme.outline)

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions().spacing16x)
        ) {
            suggestedTags.forEach { tag ->
                item {
                    WireFilterChip(
                        modifier = Modifier.padding(
                            end = dimensions().spacing8x,
                            bottom = dimensions().spacing8x
                        ),
                        label = tag,
                        isSelected = false,
                        onSelectChip = onAddTag
                    )
                }
            }
        }
    }
}

@MultipleThemePreviews
@Preview
@Composable
fun PreviewAddRemoveTagsScreen() {
    WireTheme {
        AddRemoveTagsScreenContent(
            internalPadding = PaddingValues(0.dp),
            textFieldState = TextFieldState(),
            addedTags = setOf("Android", "Web", "iOS"),
            suggestedTags = setOf("Marketing", "Finance", "HR"),
            onAddTag = {},
            onRemoveTag = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
