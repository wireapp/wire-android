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
package com.wire.android.ui.settings.devices

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.settings.devices.button.GetE2eiCertificateButton
import com.wire.android.ui.settings.devices.button.ShowE2eiCertificateButton
import com.wire.android.ui.settings.devices.button.UpdateE2eiCertificateButton
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.QualifiedClientID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.e2ei.Handle
import com.wire.kalium.logic.feature.e2ei.MLSClientE2EIStatus
import com.wire.kalium.logic.feature.e2ei.MLSClientIdentity
import com.wire.kalium.logic.feature.e2ei.MLSCredentialsType
import com.wire.kalium.logic.feature.e2ei.X509Identity
import kotlin.time.Instant

@Composable
fun EndToEndIdentityCertificateItem(
    isE2eiCertificateActivated: Boolean,
    mlsClientIdentity: MLSClientIdentity?,
    isCurrentDevice: Boolean,
    isLoadingCertificate: Boolean,
    enrollE2eiCertificate: () -> Unit,
    showCertificate: (MLSClientIdentity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(
                top = MaterialTheme.wireDimensions.spacing12x,
                bottom = MaterialTheme.wireDimensions.spacing12x,
                start = MaterialTheme.wireDimensions.spacing16x,
                end = MaterialTheme.wireDimensions.spacing12x
            )
    ) {
        Text(
            text = stringResource(id = R.string.item_title_e2ei_certificate),
            style = MaterialTheme.wireTypography.title02,
            color = MaterialTheme.wireColorScheme.onBackground
        )
        Text(
            modifier = Modifier.padding(
                top = dimensions().spacing8x,
                bottom = dimensions().spacing4x
            ),
            text = stringResource(id = R.string.item_subtitle_status_e2ei_certificate).uppercase(),
            style = MaterialTheme.wireTypography.label01,
            color = MaterialTheme.wireColorScheme.secondaryText,
        )
        Column {
            if (isE2eiCertificateActivated && mlsClientIdentity != null && mlsClientIdentity.credentialType == MLSCredentialsType.X509) {
                when (mlsClientIdentity.e2eiStatus) {
                    MLSClientE2EIStatus.REVOKED -> {
                        E2EIStatusRow(
                            label = stringResource(id = R.string.e2ei_certificat_status_revoked),
                            labelColor = colorsScheme().error,
                            icon = R.drawable.ic_certificate_revoked_mls
                        )
                        SerialNumberBlock(mlsClientIdentity.x509Identity!!.serialNumber)
                    }

                    MLSClientE2EIStatus.EXPIRED -> {
                        E2EIStatusRow(
                            label = stringResource(id = R.string.e2ei_certificat_status_expired),
                            labelColor = colorsScheme().error,
                            icon = R.drawable.ic_certificate_not_activated_mls
                        )
                        SerialNumberBlock(mlsClientIdentity.x509Identity!!.serialNumber)
                        if (isCurrentDevice) {
                            UpdateE2eiCertificateButton(
                                enabled = true,
                                isLoading = isLoadingCertificate,
                                onUpdateCertificateClicked = enrollE2eiCertificate
                            )
                        }
                    }

                    MLSClientE2EIStatus.VALID -> {
                        E2EIStatusRow(
                            label = stringResource(id = R.string.e2ei_certificat_status_valid),
                            labelColor = colorsScheme().positive,
                            icon = R.drawable.ic_certificate_valid_mls
                        )
                        SerialNumberBlock(mlsClientIdentity.x509Identity!!.serialNumber)
                        if (isCurrentDevice) {
                            UpdateE2eiCertificateButton(
                                enabled = true,
                                isLoading = isLoadingCertificate,
                                onUpdateCertificateClicked = enrollE2eiCertificate
                            )
                        }
                    }
                    MLSClientE2EIStatus.NOT_ACTIVATED -> {
                        E2EIStatusRow(
                            label = stringResource(id = R.string.e2ei_certificat_status_not_activated),
                            labelColor = colorsScheme().error,
                            icon = R.drawable.ic_certificate_not_activated_mls
                        )
                        if (isCurrentDevice) {
                            GetE2eiCertificateButton(
                                enabled = true,
                                isLoading = isLoadingCertificate,
                                onGetCertificateClicked = enrollE2eiCertificate
                            )
                        }
                    }
                }
                ShowE2eiCertificateButton(
                    enabled = true,
                    isLoading = false,
                    onShowCertificateClicked = {
                        showCertificate(mlsClientIdentity)
                    }
                )
            } else {
                E2EIStatusRow(
                    label = stringResource(id = R.string.e2ei_certificat_status_not_activated),
                    labelColor = colorsScheme().error,
                    icon = R.drawable.ic_certificate_not_activated_mls
                )
                if (isCurrentDevice) {
                    GetE2eiCertificateButton(
                        enabled = true,
                        isLoading = isLoadingCertificate,
                        onGetCertificateClicked = enrollE2eiCertificate
                    )
                }
            }
        }
    }
}

@Composable
private fun SerialNumberBlock(serialNumber: String) {
    Column {
        Text(
            modifier = Modifier.padding(
                top = dimensions().spacing8x,
                bottom = dimensions().spacing4x
            ),
            text = stringResource(id = R.string.item_subtitle_serial_number_e2ei_certificate).uppercase(),
            style = MaterialTheme.wireTypography.label01,
            color = MaterialTheme.wireColorScheme.secondaryText,
        )
        val updatedSerialNumber = serialNumber
            .replaceRange(24, 24, "\n")
            .replace(":", " : ")

        Text(
            text = updatedSerialNumber.uppercase(),
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.onBackground,
        )
    }
}

@Composable
private fun E2EIStatusRow(
    label: String,
    labelColor: Color,
    icon: Int,
    iconContentDescription: String = ""
) {
    Row {
        Text(
            modifier = Modifier.padding(end = dimensions().spacing4x),
            text = label,
            style = MaterialTheme.wireTypography.body02,
            color = labelColor,
        )
        Image(
            modifier = Modifier.align(Alignment.CenterVertically),
            painter = painterResource(id = icon),
            contentDescription = iconContentDescription,
            colorFilter = ColorFilter.tint(labelColor)
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewEndToEndIdentityCertificateItem() = WireTheme {
    EndToEndIdentityCertificateItem(
        isE2eiCertificateActivated = true,
        isCurrentDevice = false,
        mlsClientIdentity = previewMLSClientIdentity(),
        isLoadingCertificate = false,
        enrollE2eiCertificate = {},
        showCertificate = {}
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewEndToEndIdentityCertificateSelfItem() = WireTheme {
    EndToEndIdentityCertificateItem(
        isE2eiCertificateActivated = true,
        isCurrentDevice = true,
        mlsClientIdentity = previewMLSClientIdentity(),
        isLoadingCertificate = false,
        enrollE2eiCertificate = {},
        showCertificate = {}
    )
}

internal fun previewMLSClientIdentity() = MLSClientIdentity(
    clientId = QualifiedClientID(ClientId(""), UserId("", "")),
    e2eiStatus = MLSClientE2EIStatus.VALID,
    thumbprint = "thumbprint",
    credentialType = MLSCredentialsType.X509,
    x509Identity = X509Identity(
        handle = Handle("", "", ""),
        displayName = "",
        domain = "",
        certificate = "",
        serialNumber = "e5:d5:e6:75:7e:04:86:07:14:3c:a0:ed:9a:8d:e4:fd",
        notBefore = Instant.DISTANT_PAST,
        notAfter = Instant.DISTANT_FUTURE
    )
)
