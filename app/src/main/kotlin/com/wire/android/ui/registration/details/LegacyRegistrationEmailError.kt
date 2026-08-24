/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.registration.details

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.wire.android.R
import com.wire.android.feature.authentication.R as AuthenticationR
import com.wire.android.ui.authentication.legacyregistration.details.LegacyRegistrationDetailsState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.SupportPage
import com.wire.android.util.supportUrlResource

@Composable
internal fun LegacyRegistrationEmailError(
    error: LegacyRegistrationDetailsState.DetailsError,
) {
    val errorText = error.emailErrorText() ?: return
    val context = LocalContext.current
    val learnMoreUrl = supportUrlResource(SupportPage.CREATE_ACCOUNT)
    val annotatedText = buildAnnotatedString {
        append(errorText)
        if (error is LegacyRegistrationDetailsState.DetailsError.EmailFieldError.AlreadyInUseError) {
            append(" ")
            pushStringAnnotation(tag = LEARN_MORE_TAG, annotation = learnMoreUrl)
            withStyle(
                SpanStyle(
                    color = MaterialTheme.wireColorScheme.onBackground,
                    fontWeight = MaterialTheme.wireTypography.label05.fontWeight,
                    fontSize = MaterialTheme.wireTypography.label05.fontSize,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append(stringResource(R.string.label_learn_more))
            }
            pop()
        }
    }
    ClickableText(
        text = annotatedText,
        style = MaterialTheme.wireTypography.label04.copy(
            color = MaterialTheme.wireColorScheme.error,
            textAlign = TextAlign.Start,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = MaterialTheme.wireDimensions.spacing8x,
                horizontal = MaterialTheme.wireDimensions.spacing16x,
            ),
        onClick = { offset ->
            annotatedText.getStringAnnotations(LEARN_MORE_TAG, offset, offset)
                .firstOrNull()
                ?.let { CustomTabsHelper.launchUrl(context, learnMoreUrl) }
        },
    )
}

@Composable
private fun LegacyRegistrationDetailsState.DetailsError.emailErrorText(): String? = when (this) {
    LegacyRegistrationDetailsState.DetailsError.EmailFieldError.AlreadyInUseError ->
        stringResource(AuthenticationR.string.create_account_email_already_in_use_error)
    LegacyRegistrationDetailsState.DetailsError.EmailFieldError.BlacklistedEmailError ->
        stringResource(AuthenticationR.string.create_account_email_blacklisted_error)
    LegacyRegistrationDetailsState.DetailsError.EmailFieldError.DomainBlockedError ->
        stringResource(AuthenticationR.string.create_account_email_domain_blocked_error)
    LegacyRegistrationDetailsState.DetailsError.EmailFieldError.InvalidEmailError ->
        stringResource(AuthenticationR.string.create_account_email_invalid_error)
    else -> null
}

private const val LEARN_MORE_TAG = "learn_more"
