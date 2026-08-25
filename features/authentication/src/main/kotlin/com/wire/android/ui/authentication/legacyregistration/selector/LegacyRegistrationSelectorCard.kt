/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration.selector

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireTypography

@Composable
internal fun CardHeader(card: LegacyRegistrationSelectorText.Card) {
    Column {
        Text(
            text = card.title.uppercase(),
            style = MaterialTheme.wireTypography.title03,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = dimensions().spacing8x)
                .fillMaxWidth()
                .semantics { heading() },
        )
        Text(
            text = card.subtitle,
            style = MaterialTheme.wireTypography.body01,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = dimensions().spacing8x)
                .fillMaxWidth(),
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = dimensions().spacing16x))
    }
}

@Composable
internal fun CardHighlight(
    text: String,
    checkIcon: Painter,
    positiveColor: Color,
) {
    Column {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .padding(horizontal = dimensions().spacing16x)
                .fillMaxWidth(),
        ) {
            Icon(
                painter = checkIcon,
                contentDescription = null,
                modifier = Modifier.size(dimensions().spacing16x),
                tint = positiveColor,
            )
            Text(
                text = text,
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier.padding(start = dimensions().spacing8x),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = dimensions().spacing12x))
    }
}

@Composable
internal fun CardButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = dimensions().spacing8x)
    if (primary) {
        WirePrimaryButton(text = text, onClick = onClick, modifier = modifier)
    } else {
        WireSecondaryButton(text = text, onClick = onClick, modifier = modifier)
    }
}
