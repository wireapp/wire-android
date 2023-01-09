package com.wire.android.ui.home.newconversation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.ui.common.button.IconAlignment
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions

@Composable
fun SelectParticipantsButtonsAlwaysEnabled(
    count: Int = 0,
    mainButtonText: String,
    elevation: Dp = MaterialTheme.wireDimensions.bottomNavigationShadowElevation,
    modifier: Modifier = Modifier
        .padding(horizontal = dimensions().spacing16x)
        .height(dimensions().groupButtonHeight)
        .fillMaxWidth(),
    onMoreButtonClick: (() -> Unit)? = null,
    onMainButtonClick: () -> Unit,
) {
    SelectParticipantsButtonsRow(count, mainButtonText, true, elevation, modifier, onMoreButtonClick, onMainButtonClick)
}

@Composable
fun SelectParticipantsButtonsRow(
    count: Int = 0,
    mainButtonText: String,
    elevation: Dp = MaterialTheme.wireDimensions.bottomNavigationShadowElevation,
    modifier: Modifier = Modifier
        .padding(horizontal = dimensions().spacing16x)
        .height(dimensions().groupButtonHeight)
        .fillMaxWidth(),
    onMoreButtonClick: (() -> Unit)? = null,
    onMainButtonClick: () -> Unit,
) {
    SelectParticipantsButtonsRow(count, mainButtonText, false, elevation, modifier, onMoreButtonClick, onMainButtonClick)
}

@Composable
private fun SelectParticipantsButtonsRow(
    count: Int = 0,
    mainButtonText: String,
    shouldAllowNoSelectionContinue: Boolean = false,
    elevation: Dp = MaterialTheme.wireDimensions.bottomNavigationShadowElevation,
    modifier: Modifier = Modifier
        .padding(horizontal = dimensions().spacing16x)
        .height(dimensions().groupButtonHeight)
        .fillMaxWidth(),
    onMoreButtonClick: (() -> Unit)? = null,
    onMainButtonClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.wireColorScheme.background,
        shadowElevation = elevation
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
        ) {
            WirePrimaryButton(
                text = "$mainButtonText ($count)",
                onClick = onMainButtonClick,
                state = computeButtonState(count, shouldAllowNoSelectionContinue),
                blockUntilSynced = true,
                modifier = Modifier.weight(1f)
            )
            if (onMoreButtonClick != null) {
                Spacer(Modifier.width(dimensions().spacing8x))
                WireSecondaryButton(
                    onClick = onMoreButtonClick,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = stringResource(R.string.content_description_right_arrow),
                            modifier = Modifier
                                .size(dimensions().wireIconButtonSize)
                        )
                    },
                    leadingIconAlignment = IconAlignment.Center,
                    fillMaxWidth = false
                )
            }
        }
    }
}

private fun computeButtonState(count: Int = 0, shouldAllowNoSelectionContinue: Boolean): WireButtonState {
    return when {
        shouldAllowNoSelectionContinue -> WireButtonState.Default
        count > 0 -> WireButtonState.Default
        else -> WireButtonState.Disabled
    }
}

@Preview
@Composable
private fun SelectParticipantsButtonsRowPreview() {
    SelectParticipantsButtonsRow(count = 3, mainButtonText = "Continue", onMainButtonClick = {}, onMoreButtonClick = {})
}

@Preview
@Composable
private fun SelectParticipantsButtonsRowWithoutMoreButtonPreview() {
    SelectParticipantsButtonsRow(count = 3, mainButtonText = "Continue", onMainButtonClick = {})
}

@Preview
@Composable
private fun SelectParticipantsButtonsRowDisabledButtonPreview() {
    SelectParticipantsButtonsRow(count = 0, mainButtonText = "Continue", onMainButtonClick = {})
}
