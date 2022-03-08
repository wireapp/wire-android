package com.wire.android.ui.authentication.create.overview

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.authentication.create.personalaccount.CreatePersonalAccountViewModel
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper

@Composable
fun CreateAccountOverviewScreen(viewModel: CreatePersonalAccountViewModel, overviewParams: CreateAccountOverviewParams) {
    OverviewContent(
        onBackPressed = viewModel::goBackToPreviousStep,
        onContinuePressed = viewModel::onOverviewContinue,
        overviewParams = overviewParams
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewContent(onBackPressed: () -> Unit, onContinuePressed: () -> Unit, overviewParams: CreateAccountOverviewParams) {
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = overviewParams.title,
                onNavigationPressed = onBackPressed
            )
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val context = LocalContext.current
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(id = overviewParams.contentIconResId),
                contentDescription = "",
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .padding(
                        horizontal = MaterialTheme.wireDimensions.spacing64x,
                        vertical = MaterialTheme.wireDimensions.spacing32x
                    )
            )
            OverviewTexts(
                modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing24x),
                onLearnMoreClick = { CustomTabsHelper.launchUrl(context, overviewParams.learnMoreUrl) },
                overviewParams = overviewParams,
            )
            Spacer(modifier = Modifier.weight(1f))
            WirePrimaryButton(
                text = stringResource(R.string.label_continue),
                onClick = onContinuePressed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.wireDimensions.spacing16x),
            )
        }
    }
}

@Composable
private fun OverviewTexts(overviewParams: CreateAccountOverviewParams, modifier: Modifier, onLearnMoreClick: () -> Unit) {
    Column(modifier = modifier) {
        if(overviewParams.contentTitle.isNotEmpty())
            Text(
                text = overviewParams.contentTitle,
                style = MaterialTheme.wireTypography.title01,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.wireDimensions.spacing8x)
            )
        Text(
            text = overviewParams.contentText,
            style = MaterialTheme.wireTypography.body02,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = overviewParams.learnMoreText,
            style = MaterialTheme.wireTypography.body02.copy(
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colorScheme.primary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onLearnMoreClick
                )
        )
    }
}

@Composable
@Preview
private fun CreateAccountOverviewScreenPreview() {
    OverviewContent(
        onBackPressed = { },
        onContinuePressed = { },
        overviewParams = CreateAccountOverviewParams(
            title ="title",
            contentTitle = "contentTitle",
            contentText = "contentText",
            contentIconResId = R.drawable.ic_create_personal_account,
            learnMoreText = "learn more",
            learnMoreUrl = ""
        )
    )
}
