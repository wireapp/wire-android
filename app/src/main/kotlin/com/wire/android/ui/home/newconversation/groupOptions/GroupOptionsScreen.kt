package com.wire.android.ui.home.newconversation.groupOptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.R
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.WireLabeledSwitch
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions

@Composable
fun GroupOptionScreen(
    groupOptionState: GroupOptionState,
    onAllowGuestChanged: ((Boolean) -> Unit),
    onAllowServicesChanged: ((Boolean) -> Unit),
    onReadReceiptChanged: ((Boolean) -> Unit),
    onCreateGroup: () -> Unit,
    onBackPressed: () -> Unit
) {
    GroupOptionScreenContent(
        groupOptionState = groupOptionState,
        onAllowGuestChanged = onAllowGuestChanged,
        onAllowServicesChanged = onAllowServicesChanged,
        onReadReceiptChanged = onReadReceiptChanged,
        onContinuePressed = onCreateGroup,
        onBackPressed = onBackPressed
    )
}

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun GroupOptionScreenContent(
    groupOptionState: GroupOptionState,
    onAllowGuestChanged: ((Boolean) -> Unit),
    onAllowServicesChanged: ((Boolean) -> Unit),
    onReadReceiptChanged: ((Boolean) -> Unit),
    onContinuePressed: () -> Unit,
    onBackPressed: () -> Unit,
) {
    with(groupOptionState) {
        Scaffold(topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onBackPressed,
                elevation = 0.dp,
                title = stringResource(id = R.string.new_group_title)
            )
        }) { internalPadding ->
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(internalPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val (
                    button,
                    allowGuests, allowGuestsDescription,
                    allowServices, allowServicesDescription,
                    readReceipt, readReceiptDescription) = createRefs()
                WireLabeledSwitch(
                    isAllowGuestEnabled, { onAllowGuestChanged.invoke(it) }, stringResource(R.string.allow_guests),
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .constrainAs(allowGuests) {
                            top.linkTo(parent.top)
                        }
                )

                Text(
                    text = stringResource(R.string.allow_guest_switch_description),
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    modifier = Modifier
                        .padding(16.dp)
                        .constrainAs(allowGuestsDescription) {
                            top.linkTo(allowGuests.bottom)
                        },
                    textAlign = TextAlign.Left,
                    fontSize = 16.sp
                )

                WireLabeledSwitch(
                    isAllowServicesEnabled, { onAllowServicesChanged.invoke(it) }, stringResource(R.string.allow_services),
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .constrainAs(allowServices) {
                            top.linkTo(allowGuestsDescription.bottom)
                        }
                )

                Text(
                    text = stringResource(R.string.allow_services_description),
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    modifier = Modifier
                        .padding(16.dp)
                        .constrainAs(allowServicesDescription) {
                            top.linkTo(allowServices.bottom)
                        },
                    textAlign = TextAlign.Left,
                    fontSize = 16.sp
                )

                WireLabeledSwitch(
                    isReadReceiptEnabled, { onReadReceiptChanged.invoke(it) }, stringResource(R.string.read_receipts),
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .constrainAs(readReceipt) {
                            top.linkTo(allowServicesDescription.bottom)
                        }
                )

                Text(
                    text = stringResource(R.string.read_receipts_description),
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.wireColorScheme.secondaryText,
                    modifier = Modifier
                        .padding(16.dp)
                        .constrainAs(readReceiptDescription) {
                            top.linkTo(readReceipt.bottom)
                        },
                    textAlign = TextAlign.Left,
                    fontSize = 16.sp
                )



                WirePrimaryButton(
                    text = stringResource(R.string.label_continue),
                    onClick = onContinuePressed,
                    fillMaxWidth = true,
                    loading = isLoading,
                    trailingIcon = Icons.Filled.ChevronRight.Icon(),
                    state = if (continueEnabled) WireButtonState.Default else WireButtonState.Disabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.wireDimensions.spacing16x)
                        .constrainAs(button) {
                            bottom.linkTo(parent.bottom)
                        }
                )
            }
        }
    }
}

@Composable
@Preview
private fun GroupOptionScreenPreview() {
    GroupOptionScreenContent(
        GroupOptionState(),
        {},
        {}, {}, {}, {}
    )
}
