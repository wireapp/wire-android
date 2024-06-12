/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.feature.e2ei.MLSClientE2EIStatus

@Composable
fun RowScope.ConversationVerificationIcons(
    protocolInfo: Conversation.ProtocolInfo?,
    mlsVerificationStatus: Conversation.VerificationStatus?,
    proteusVerificationStatus: Conversation.VerificationStatus?
) {
    val mlsIcon: @Composable () -> Unit = {
        if (mlsVerificationStatus == Conversation.VerificationStatus.VERIFIED) {
            MLSVerifiedIcon(
                contentDescriptionId = R.string.content_description_mls_certificate_valid,
                modifier = Modifier
                    .wrapContentWidth()
                    .align(Alignment.CenterVertically)
            )
        }
    }
    val proteusIcon: @Composable () -> Unit = {
        if (proteusVerificationStatus == Conversation.VerificationStatus.VERIFIED) {
            ProteusVerifiedIcon(
                contentDescriptionId = R.string.content_description_proteus_certificate_valid,
                modifier = Modifier
                    .wrapContentWidth()
                    .align(Alignment.CenterVertically)
            )
        }
    }

    if (protocolInfo is Conversation.ProtocolInfo.Proteus) {
        proteusIcon()
        mlsIcon()
    } else {
        mlsIcon()
        proteusIcon()
    }
}

@Composable
fun RowScope.MLSVerificationIcon(mlsVerificationStatus: MLSClientE2EIStatus?) {
    val mlsIconModifier = Modifier
        .wrapContentWidth()
        .align(Alignment.CenterVertically)

    when (mlsVerificationStatus) {
        MLSClientE2EIStatus.VALID -> MLSVerifiedIcon(
            contentDescriptionId = R.string.e2ei_certificat_status_valid,
            modifier = mlsIconModifier
        )

        MLSClientE2EIStatus.REVOKED -> MLSRevokedIcon(modifier = mlsIconModifier)

        MLSClientE2EIStatus.EXPIRED
        -> MLSNotVerifiedIcon(
            contentDescriptionId = R.string.e2ei_certificat_status_expired,
            modifier = mlsIconModifier
        )

        MLSClientE2EIStatus.NOT_ACTIVATED -> MLSNotVerifiedIcon(modifier = mlsIconModifier)

        null -> MLSNotVerifiedIcon(modifier = mlsIconModifier)
    }
}

@Composable
fun ProteusVerifiedIcon(
    modifier: Modifier = Modifier,
    @StringRes contentDescriptionId: Int = R.string.label_client_verified
) {
    Image(
        modifier = modifier.padding(start = dimensions().spacing4x),
        painter = painterResource(id = R.drawable.ic_certificate_valid_proteus),
        contentDescription = stringResource(contentDescriptionId)
    )
}

@Composable
fun MLSVerifiedIcon(
    modifier: Modifier = Modifier,
    @StringRes contentDescriptionId: Int = R.string.label_client_verified
) {
    Image(
        modifier = modifier.padding(start = dimensions().spacing4x),
        painter = painterResource(id = R.drawable.ic_certificate_valid_mls),
        contentDescription = stringResource(contentDescriptionId)
    )
}

@Composable
fun MLSRevokedIcon(
    modifier: Modifier = Modifier,
    @StringRes contentDescriptionId: Int = R.string.e2ei_certificat_status_revoked
) {
    Image(
        modifier = modifier.padding(start = dimensions().spacing4x),
        painter = painterResource(id = R.drawable.ic_certificate_revoked_mls),
        contentDescription = stringResource(contentDescriptionId)
    )
}

@Composable
fun MLSNotVerifiedIcon(
    modifier: Modifier = Modifier,
    @StringRes contentDescriptionId: Int = R.string.e2ei_certificat_status_not_activated
) {
    Image(
        modifier = modifier.padding(start = dimensions().spacing4x),
        painter = painterResource(id = R.drawable.ic_certificate_not_activated_mls),
        contentDescription = stringResource(contentDescriptionId)
    )
}
