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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.R
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.WireLabeledSwitch
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions

@Composable
fun GroupOptionScreen(
    groupOptionState: GroupOptionState,
    onCreateGroup: () -> Unit,
    onBackPressed: () -> Unit
) {
    GroupOptionScreenContent(
        groupOptionState = groupOptionState,
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
            ) {
                val (button) = createRefs()
                WireLabeledSwitch(
                    true, {}, "text",
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                )

                WireLabeledSwitch(
                    true, {}, "text",
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                )


                WireLabeledSwitch(
                    true, {}, "text",
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
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
private fun NewGroupScreenPreview() {
    GroupOptionScreenContent(
        GroupOptionState(),
        {},
        {},
    )
}
