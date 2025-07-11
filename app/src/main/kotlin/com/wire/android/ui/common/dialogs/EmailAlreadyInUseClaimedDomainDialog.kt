/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.common.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.R
import com.wire.android.ui.authentication.login.DomainClaimedByOrg
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.typography
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun EmailAlreadyInUseClaimedDomainDialog(
    dialogState: VisibilityState<DomainClaimedByOrg.Claimed>,
    onDismiss: () -> Unit,
) {
    VisibilityState(dialogState) { state ->
        WireDialog(
            title = stringResource(R.string.claimed_domain_email_already_in_use_dialog_title),
            text = stringResource(R.string.claimed_domain_email_already_in_use_dialog_description, state.domain),
            content = {
                Content()
            },
            onDismiss = onDismiss,
            buttonsHorizontalAlignment = false,
            properties = wireDialogPropertiesBuilder(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = onDismiss,
                text = stringResource(id = R.string.label_ok),
                type = WireDialogButtonType.Primary,
                state = WireButtonState.Default
            ),
        )
    }
}

@Composable
private fun Content() {
    Column {
        Row {
            val bullet = stringResource(R.string.bullet_finger_point) + "  "
            val changeEmail = stringResource(id = R.string.claimed_domain_change_email_label) + "\n"
            val deletePersonalAccount = stringResource(id = R.string.claimed_domain_delete_personal_account)
            val fullAnnotatedString = buildAnnotatedString {
                append(bullet)
                append(changeEmail)
                append(bullet)
                append(deletePersonalAccount)

                addStyledLink(
                    url = stringResource(id = R.string.url_change_email),
                    start = bullet.length,
                    end = (bullet + changeEmail).length
                )
                addStyledLink(
                    url = stringResource(id = R.string.url_delete_personal_account),
                    start = (bullet + changeEmail + bullet).length,
                    end = (bullet + changeEmail + bullet + deletePersonalAccount).length
                )
            }
            Text(
                text = fullAnnotatedString,
                style = typography().body01,
                color = colorsScheme().onSurface,
            )
        }
    }
}

@Composable
private fun AnnotatedString.Builder.addStyledLink(url: String, start: Int, end: Int) {
    val context = LocalContext.current
    val linkStyle = SpanStyle(
        fontSize = typography().body02.fontSize,
        fontWeight = typography().body02.fontWeight,
        fontStyle = typography().body02.fontStyle,
        color = colorsScheme().onSurface,
        textDecoration = TextDecoration.Underline,
    )
    val textLinkStyles = TextLinkStyles(
        style = linkStyle,
        focusedStyle = linkStyle,
        pressedStyle = linkStyle,
        hoveredStyle = linkStyle
    )
    addStyle(
        style = linkStyle,
        start = start,
        end = end,
    )
    addLink(
        url = LinkAnnotation.Url(
            url = url,
            styles = textLinkStyles,
            linkInteractionListener = {
                CustomTabsHelper.launchUrl(context, url)
            }
        ),
        start = start,
        end = end,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewEmailAlreadyInUseClaimedDomainDialog() = WireTheme {
    EmailAlreadyInUseClaimedDomainDialog(VisibilityState(isVisible = true, saveable = DomainClaimedByOrg.Claimed("domain.com"))) {
    }
}
