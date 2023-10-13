/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.settings.devices.button.GetE2eiCertificateButton
import com.wire.android.ui.settings.devices.button.ShowE2eiCertificateButton
import com.wire.android.ui.settings.devices.button.UpdateE2eiCertificateButton
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.feature.e2ei.CertificateStatus
import com.wire.kalium.logic.feature.e2ei.E2eiCertificate

@Composable
fun EndToEndIdentityCertificateItem(
    isE2eiCertificateActivated: Boolean,
    certificate: E2eiCertificate,
    isSelfClient: Boolean,
    enrollE2eiCertificate: () -> Unit,
    updateE2eiCertificate: () -> Unit,
    showCertificate: () -> Unit
) {
    Column(
        modifier = Modifier
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
            if (isE2eiCertificateActivated) {
                when (certificate.status) {
                    CertificateStatus.REVOKED -> {
                        E2EIStatusRow(
                            label = stringResource(id = R.string.e2ei_certificat_status_revoked),
                            labelColor = colorsScheme().error,
                            icon = R.drawable.ic_certificate_revoked_mls
                        )
                        SerialNumberBlock(certificate.serialNumber)
                        ShowE2eiCertificateButton(
                            enabled = true,
                            isLoading = false,
                            showCertificate
                        )
                    }

                    CertificateStatus.EXPIRED -> {
                        E2EIStatusRow(
                            label = stringResource(id = R.string.e2ei_certificat_status_expired),
                            labelColor = colorsScheme().error,
                            icon = R.drawable.ic_certificate_not_activated_mls
                        )
                        SerialNumberBlock(certificate.serialNumber)
                        UpdateE2eiCertificateButton(
                            enabled = true,
                            isLoading = false,
                            updateE2eiCertificate
                        )
                        ShowE2eiCertificateButton(
                            enabled = true,
                            isLoading = false,
                            showCertificate
                        )
                    }

                    CertificateStatus.VALID -> {
                        E2EIStatusRow(
                            label = stringResource(id = R.string.e2ei_certificat_status_valid),
                            labelColor = colorsScheme().validE2eiStatusColor,
                            icon = R.drawable.ic_certificate_valid_mls
                        )
                        SerialNumberBlock(certificate.serialNumber)
                        ShowE2eiCertificateButton(
                            enabled = true,
                            isLoading = false,
                            showCertificate
                        )
                    }
                }
            } else {
                E2EIStatusRow(
                    label = stringResource(id = R.string.e2ei_certificat_status_not_activated),
                    labelColor = colorsScheme().error,
                    icon = R.drawable.ic_certificate_not_activated_mls
                )
                if (isSelfClient) {
                    GetE2eiCertificateButton(enabled = true, isLoading = false) { }
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
            painter = painterResource(id = icon),
            contentDescription = iconContentDescription,
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewEndToEndIdentityCertificateItem() {
    EndToEndIdentityCertificateItem(
        isE2eiCertificateActivated = true,
        isSelfClient = false,
        certificate = E2eiCertificate(
            issuer = "Wire",
            status = CertificateStatus.VALID,
            serialNumber = "e5:d5:e6:75:7e:04:86:07:14:3c:a0:ed:9a:8d:e4:fd",
            certificateDetail = ""
        ),
        enrollE2eiCertificate = {},
        updateE2eiCertificate = {},
        showCertificate = {}
    )
}
