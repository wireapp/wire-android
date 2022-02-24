package com.wire.android.ui.authentication.create.personalaccount

import android.content.Context
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
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.kalium.logic.configuration.ServerConfig

@Composable
fun OverviewScreen(
    navigationManager: CreatePersonalAccountNavigationManager,
    serverConfig: ServerConfig,
) {
    OverviewContent(
        onBackPressed = { navigationManager.navigateBack() },
        onContinuePressed = { navigationManager.navigate(CreatePersonalAccountDestination.Email) },
        websiteBaseUrl = serverConfig.websiteUrl,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewContent(
    onBackPressed: () -> Unit,
    onContinuePressed: () -> Unit,
    websiteBaseUrl: String,
) {
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = stringResource(R.string.create_personal_account_title),
                onNavigationPressed = onBackPressed
            )
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val context = LocalContext.current
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(id = R.drawable.ic_create_personal_account),
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
                onLearnMoreClick = { openLearnMorePage(context, websiteBaseUrl) }
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
private fun OverviewTexts(modifier: Modifier, onLearnMoreClick: () -> Unit) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.create_personal_account_text),
            style = MaterialTheme.wireTypography.body02,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.create_personal_account_link),
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

private fun openLearnMorePage(context: Context, websiteBaseUrl: String) {
    val url = "https://${websiteBaseUrl}/pricing"
    CustomTabsHelper.launchUrl(context, url)
}

@Composable
@Preview
private fun OverviewScreenPreview() {
    OverviewContent(
        onBackPressed = { },
        onContinuePressed = { },
        websiteBaseUrl = ""
    )
}
