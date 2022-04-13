package com.wire.android.ui.authentication.create.summary

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun CreateAccountSummaryScreen(viewModel: CreateAccountSummaryViewModel) {
    SummaryContent(
        state = viewModel.summaryState,
        onContinuePressed = viewModel::onSummaryContinue
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryContent(
    state: CreateAccountSummaryViewState,
    onContinuePressed: () -> Unit
) {
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = stringResource(id = state.type.titleResId),
                navigationIconType = null
            )
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(id = state.type.summaryResources.summaryIconResId),
                contentDescription = stringResource(id = R.string.content_description_create_account_summary),
                contentScale = ContentScale.Inside,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.wireDimensions.spacing64x,
                    vertical = MaterialTheme.wireDimensions.spacing32x
                )
            )
            Text(
                text = stringResource(id = state.type.summaryResources.summaryTextResId),
                style = MaterialTheme.wireTypography.body02,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.wireDimensions.spacing24x)
            )
            Spacer(modifier = Modifier.weight(1f))
            WirePrimaryButton(
                text = stringResource(R.string.label_get_started),
                onClick = onContinuePressed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.wireDimensions.spacing16x)
            )
        }
    }
}

@Preview
@Composable
private fun CreateAccountSummaryScreenPreview() {
    SummaryContent(CreateAccountSummaryViewState(CreateAccountFlowType.CreatePersonalAccount), {})
}
