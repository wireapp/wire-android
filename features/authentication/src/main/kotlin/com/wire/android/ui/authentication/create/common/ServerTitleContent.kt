/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.authentication.create.common

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.model.Clickable
import com.wire.android.ui.common.R as CommonR
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.clickable
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Immutable
data class ServerTitlePresentation(
    val text: String,
    val showFullText: Boolean,
    val infoContentDescription: String,
    val detailsTitle: String,
    val detailsBody: AnnotatedString,
    val confirmLabel: String,
)

@Composable
fun ServerTitleContent(
    presentation: ServerTitlePresentation,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.wireTypography.title01,
    textColor: Color = MaterialTheme.wireColorScheme.secondaryText,
    infoIconColor: Color = MaterialTheme.wireColorScheme.secondaryText,
) {
    var detailsVisible by remember { mutableStateOf(false) }
    val iconSizeDp = MaterialTheme.wireDimensions.wireIconButtonSize
    val iconSizeSp = with(LocalDensity.current) { iconSizeDp.toSp() }
    val infoIconId = "info"
    val annotatedText = buildAnnotatedString {
        append(presentation.text)
        append(" ")
        appendInlineContent(infoIconId, "[info]")
    }
    val inlineContent = mapOf(
        infoIconId to InlineTextContent(
            Placeholder(
                width = iconSizeSp,
                height = iconSizeSp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            )
        ) {
            Icon(
                painter = painterResource(CommonR.drawable.ic_info),
                contentDescription = presentation.infoContentDescription,
                modifier = Modifier
                    .size(iconSizeDp)
                    .clickable(Clickable(enabled = true, onClick = { detailsVisible = true })),
                tint = infoIconColor,
            )
        }
    )

    Text(
        text = annotatedText,
        style = style,
        color = textColor,
        maxLines = if (presentation.showFullText) Int.MAX_VALUE else 1,
        overflow = TextOverflow.Ellipsis,
        inlineContent = inlineContent,
        modifier = modifier.semantics { heading() },
    )

    if (detailsVisible) {
        WireDialog(
            title = presentation.detailsTitle,
            text = presentation.detailsBody,
            onDismiss = { detailsVisible = false },
            optionButton1Properties = WireDialogButtonProperties(
                text = presentation.confirmLabel,
                onClick = { detailsVisible = false },
                type = WireDialogButtonType.Primary,
            ),
        )
    }
}
