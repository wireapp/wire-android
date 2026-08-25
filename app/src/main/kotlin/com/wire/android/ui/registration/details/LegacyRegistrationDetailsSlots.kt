/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.registration.details

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import com.wire.android.R
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.ui.authentication.create.common.ServerTitle
import com.wire.android.ui.common.WireCheckbox
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.isHostValidForAnalytics
import com.wire.kalium.logic.configuration.server.ServerConfig

@Composable
internal fun LegacyRegistrationServerTitle(serverConfig: ServerConfig.Links) {
    if (serverConfig.isOnPremises) {
        ServerTitle(
            serverLinks = serverConfig,
            style = MaterialTheme.wireTypography.body01,
        )
    }
}

@Composable
internal fun LegacyRegistrationPrivacyPolicy(
    serverConfig: ServerConfig.Links,
    accepted: Boolean,
    onAccepted: (Boolean) -> Unit,
) {
    if (!serverConfig.isHostValidForAnalytics()) return
    Row(modifier = Modifier.padding(end = MaterialTheme.wireDimensions.spacing16x)) {
        WireCheckbox(checked = accepted, onCheckedChange = onAccepted)
        PrivacyPolicyLink()
    }
}

@Composable
private fun RowScope.PrivacyPolicyLink() {
    val context = LocalContext.current
    val url = androidx.compose.ui.res.stringResource(R.string.url_privacy_policy)
    val text = buildAnnotatedString {
        append(androidx.compose.ui.res.stringResource(AuthenticationR.string.create_account_email_share_anonymous_data_label))
        append(" ")
        withLink(
            LinkAnnotation.Clickable(
                tag = "privacyPolicy",
                styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline)),
                linkInteractionListener = { CustomTabsHelper.launchUrl(context, url) },
            ),
        ) {
            append(androidx.compose.ui.res.stringResource(AuthenticationR.string.create_account_email_share_anonymous_data_link_label))
        }
        append(" ")
        append(androidx.compose.ui.res.stringResource(AuthenticationR.string.create_account_email_share_anonymous_data_optional_label))
    }
    Text(
        text = text,
        style = MaterialTheme.wireTypography.label04,
        textAlign = TextAlign.Start,
        modifier = Modifier.align(Alignment.CenterVertically),
    )
}

@Composable
internal fun RowScope.LegacyRegistrationTeamBackLink(teamCreationUrl: String) {
    val context = LocalContext.current
    val text = buildAnnotatedString {
        append(androidx.compose.ui.res.stringResource(AuthenticationR.string.create_account_email_backlink_to_team_label))
        append("\n")
        withLink(
            LinkAnnotation.Clickable(
                tag = "teamCreation",
                styles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline)),
                linkInteractionListener = { CustomTabsHelper.launchUrl(context, teamCreationUrl) },
            ),
        ) {
            append(androidx.compose.ui.res.stringResource(AuthenticationR.string.welcome_button_create_team))
        }
    }
    Text(
        text = text,
        style = MaterialTheme.wireTypography.label04,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .align(Alignment.CenterVertically)
            .padding(bottom = MaterialTheme.wireDimensions.spacing16x)
            .fillMaxWidth(),
    )
}
