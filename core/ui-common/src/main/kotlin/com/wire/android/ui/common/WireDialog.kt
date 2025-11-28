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

@file:Suppress("MultiLineIfElse")

package com.wire.android.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.WireTertiaryButton
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.theme.isTablet
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Stable
@Composable
fun wireDialogPropertiesBuilder(
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    usePlatformDefaultWidth: Boolean = isTablet
): DialogProperties = DialogProperties(
    dismissOnBackPress = dismissOnBackPress,
    dismissOnClickOutside = dismissOnClickOutside,
    usePlatformDefaultWidth = usePlatformDefaultWidth
)

@Composable
fun WireDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    textSuffixLink: DialogTextSuffixLink? = null,
    optionButton1Properties: WireDialogButtonProperties? = null,
    optionButton2Properties: WireDialogButtonProperties? = null,
    dismissButtonProperties: WireDialogButtonProperties? = null,
    buttonsHorizontalAlignment: Boolean = true,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.dialogCornerSize),
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.wireDimensions.dialogContentPadding),
    properties: DialogProperties = wireDialogPropertiesBuilder(),
    centerContent: Boolean = false,
    titleLoading: Boolean = false,
    dialogDescription: String = stringResource(R.string.content_description_alert),
    content: @Composable (() -> Unit)? = null
) {
    WireDialog(
        onDismiss = onDismiss,
        properties = properties,
        optionButton1Properties = optionButton1Properties,
        optionButton2Properties = optionButton2Properties,
        dismissButtonProperties = dismissButtonProperties,
        buttonsHorizontalAlignment = buttonsHorizontalAlignment,
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = shape,
        contentPadding = contentPadding,
        title = title,
        titleLoading = titleLoading,
        text = buildAnnotatedString {
            val style = SpanStyle(
                color = colorsScheme().onBackground,
                fontWeight = MaterialTheme.wireTypography.body01.fontWeight,
                fontSize = MaterialTheme.wireTypography.body01.fontSize,
                fontFamily = MaterialTheme.wireTypography.body01.fontFamily,
                fontStyle = MaterialTheme.wireTypography.body01.fontStyle
            )
            withStyle(style) { append(text) }
        },
        textSuffixLink = textSuffixLink,
        centerContent = centerContent,
        dialogDescription = dialogDescription,
        content = content
    )
}

@Composable
fun WireDialog(
    title: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    text: AnnotatedString? = null,
    textSuffixLink: DialogTextSuffixLink? = null,
    optionButton1Properties: WireDialogButtonProperties? = null,
    optionButton2Properties: WireDialogButtonProperties? = null,
    dismissButtonProperties: WireDialogButtonProperties? = null,
    buttonsHorizontalAlignment: Boolean = true,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.dialogCornerSize),
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.wireDimensions.dialogContentPadding),
    properties: DialogProperties = wireDialogPropertiesBuilder(),
    centerContent: Boolean = false,
    titleLoading: Boolean = false,
    dialogDescription: String = stringResource(R.string.content_description_alert),
    content: @Composable (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = properties
    ) {
        WireDialogContent(
            optionButton1Properties = optionButton1Properties,
            optionButton2Properties = optionButton2Properties,
            dismissButtonProperties = dismissButtonProperties,
            buttonsHorizontalAlignment = buttonsHorizontalAlignment,
            modifier = modifier.semantics { paneTitle = dialogDescription },
            shape = shape,
            contentPadding = contentPadding,
            title = title,
            titleLoading = titleLoading,
            text = text,
            textSuffixLink = textSuffixLink,
            centerContent = centerContent,
            content = content
        )
    }
}

@Composable
fun WireDialogContent(
    title: String?,
    modifier: Modifier = Modifier,
    titleLoading: Boolean = false,
    text: AnnotatedString? = null,
    textSuffixLink: DialogTextSuffixLink? = null,
    optionButton1Properties: WireDialogButtonProperties? = null,
    optionButton2Properties: WireDialogButtonProperties? = null,
    dismissButtonProperties: WireDialogButtonProperties? = null,
    buttonsHorizontalAlignment: Boolean = true,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.dialogCornerSize),
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.wireDimensions.dialogContentPadding),
    centerContent: Boolean = false,
    content: @Composable (() -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = modifier.padding(MaterialTheme.wireDimensions.dialogCardMargin),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            horizontalAlignment = if (centerContent) Alignment.CenterHorizontally else Alignment.Start
        ) {
            // Title
            title?.let {
                TitleDialogSection(title, titleLoading)
            }

            // Dynamic sized body content
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(
                        top = MaterialTheme.wireDimensions.dialogTextsSpacing,
                        bottom = MaterialTheme.wireDimensions.dialogTextsSpacing
                    )
                    .fillMaxWidth()
            ) {
                text?.let {
                    item {
                        TextWithLinkSuffix(
                            text = text,
                            linkText = textSuffixLink?.linkText,
                            onLinkClick = { textSuffixLink?.linkUrl?.let { uriHandler.openUri(it) } },
                            modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.dialogTextsSpacing)
                        )
                    }
                }

                content?.let {
                    item {
                        Box {
                            it.invoke()
                        }
                    }
                }
            }

            // Buttons actions
            DialogButtonsSection(dismissButtonProperties, optionButton1Properties, optionButton2Properties, buttonsHorizontalAlignment)
        }
    }
}

@Composable
private fun TitleDialogSection(title: String, titleLoading: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.wireTypography.title02, modifier = Modifier.semantics { heading() })
        if (titleLoading) {
            WireCircularProgressIndicator(progressColor = MaterialTheme.wireColorScheme.onBackground)
        }
    }
}

@Composable
private fun DialogButtonsSection(
    dismissButtonProperties: WireDialogButtonProperties?,
    optionButton1Properties: WireDialogButtonProperties?,
    optionButton2Properties: WireDialogButtonProperties?,
    buttonsHorizontalAlignment: Boolean
) {
    val containsAnyButton = dismissButtonProperties != null || optionButton1Properties != null || optionButton2Properties != null
    val dialogButtonsSpacing = if (containsAnyButton) dimensions().dialogButtonsSpacing else dimensions().spacing0x
    if (buttonsHorizontalAlignment) {
        Row(Modifier.padding(top = dialogButtonsSpacing)) {
            dismissButtonProperties.getButton(Modifier.weight(1f))
            if (dismissButtonProperties != null) {
                Spacer(Modifier.width(dialogButtonsSpacing))
            }
            optionButton1Properties.getButton(Modifier.weight(1f))
            if (optionButton2Properties != null) {
                Spacer(Modifier.width(dialogButtonsSpacing))
            }
            optionButton2Properties.getButton(Modifier.weight(1f))
        }
    } else {
        Column(Modifier.padding(top = dialogButtonsSpacing)) {
            optionButton1Properties.getButton()

            if (optionButton2Properties != null) {
                Spacer(Modifier.height(dialogButtonsSpacing))
            }
            optionButton2Properties.getButton()

            if (dismissButtonProperties != null) {
                Spacer(Modifier.height(dialogButtonsSpacing))
            }
            dismissButtonProperties.getButton()
        }
    }
}

@Composable
private fun WireDialogButtonProperties?.getButton(modifier: Modifier = Modifier) {
    this?.let {
        Box(modifier = modifier) {
            when (type) {
                WireDialogButtonType.Primary ->
                    WirePrimaryButton(onClick = onClick, text = text, state = state, loading = loading, description = description)

                WireDialogButtonType.Secondary ->
                    WireSecondaryButton(onClick = onClick, text = text, state = state, loading = loading, description = description)

                WireDialogButtonType.Tertiary ->
                    WireTertiaryButton(onClick = onClick, text = text, state = state, loading = loading, description = description)
            }
        }
    }
}

enum class WireDialogButtonType { Primary, Secondary, Tertiary }

data class WireDialogButtonProperties(
    val text: String,
    val onClick: () -> Unit,
    val state: WireButtonState = WireButtonState.Default,
    val type: WireDialogButtonType = WireDialogButtonType.Secondary,
    val loading: Boolean = false,
    val description: String? = null
)

data class DialogTextSuffixLink(val linkText: String, val linkUrl: String)
