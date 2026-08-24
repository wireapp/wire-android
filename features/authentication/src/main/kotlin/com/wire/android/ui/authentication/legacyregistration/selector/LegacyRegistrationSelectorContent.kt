/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration.selector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.wire.android.ui.newauthentication.login.NewAuthContainer
import com.wire.android.ui.newauthentication.login.NewAuthHeader
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

data class LegacyRegistrationSelectorText(
    val title: String,
    val team: Card,
    val personal: Card,
) {
    data class Card(
        val title: String,
        val subtitle: String,
        val highlights: List<String>,
        val continueLabel: String,
    )
}

@Composable
fun LegacyRegistrationSelectorContent(
    text: LegacyRegistrationSelectorText,
    checkIcon: Painter,
    positiveColor: Color,
    serverTitle: @Composable () -> Unit,
    onNavigateBack: () -> Unit,
    onPersonalAccountCreationClicked: () -> Unit,
    onTeamAccountCreationClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NewAuthContainer(
        modifier = modifier,
        header = {
            NewAuthHeader(
                title = {
                    Text(
                        text = text.title,
                        style = MaterialTheme.wireTypography.title01,
                        modifier = Modifier.semantics { heading() },
                    )
                    serverTitle()
                },
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
            )
        },
        contentPadding = dimensions().spacing16x,
    ) {
        SelectorCard(
            card = text.team,
            primary = true,
            checkIcon = checkIcon,
            positiveColor = positiveColor,
            onClick = onTeamAccountCreationClicked,
        )
        SelectorCard(
            card = text.personal,
            primary = false,
            checkIcon = checkIcon,
            positiveColor = positiveColor,
            onClick = onPersonalAccountCreationClicked,
        )
    }
}

@Composable
private fun SelectorCard(
    card: LegacyRegistrationSelectorText.Card,
    primary: Boolean,
    checkIcon: Painter,
    positiveColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(
                start = dimensions().spacing16x,
                end = dimensions().spacing16x,
                bottom = dimensions().spacing24x,
            )
            .border(
                border = BorderStroke(
                    dimensions().spacing1x,
                    if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
                shape = RoundedCornerShape(dimensions().spacing24x),
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.wireDimensions.spacing12x),
            modifier = Modifier.padding(
                horizontal = dimensions().spacing16x,
                vertical = dimensions().spacing16x,
            ),
        ) {
            CardHeader(card)
            card.highlights.forEach { highlight ->
                CardHighlight(highlight, checkIcon, positiveColor)
            }
            CardButton(card.continueLabel, primary, onClick)
        }
    }
}
