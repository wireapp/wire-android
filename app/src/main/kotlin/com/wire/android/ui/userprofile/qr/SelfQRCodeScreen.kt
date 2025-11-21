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
package com.wire.android.ui.userprofile.qr
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireRootNavGraph

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.lightspark.composeqr.DotShape
import com.lightspark.composeqr.QrCodeView
import com.wire.android.R
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ifNotEmpty
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.launch

@Destination<WireRootNavGraph>(
    style = SlideNavigationAnimation::class,
    navArgs = SelfQrCodeNavArgs::class
)
@Composable
fun SelfQRCodeScreen(
    navigator: Navigator,
    viewModel: SelfQRCodeViewModel = hiltViewModel()
) {
    if (viewModel.selfQRCodeState.hasError) {
        navigator.navigateBack()
    }
    SelfQRCodeContent(
        viewModel.selfQRCodeState,
        viewModel::shareQRAsset,
        viewModel::trackAnalyticsEvent,
        navigator::navigateBack
    )
}

@Composable
private fun SelfQRCodeContent(
    state: SelfQRCodeState,
    shareQRAssetClick: suspend (Bitmap) -> Uri,
    trackAnalyticsEvent: (AnalyticsEvent.QrCode.Modal) -> Unit,
    onBackClick: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    val context = LocalContext.current

    BackHandler {
        trackAnalyticsEvent(
            AnalyticsEvent.QrCode.Modal.Back(
                isTeam = state.isTeamMember
            )
        )
        onBackClick()
    }

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(id = R.string.user_profile_qr_code_title),
                onNavigationPressed = {
                    trackAnalyticsEvent(
                        AnalyticsEvent.QrCode.Modal.Back(
                            isTeam = state.isTeamMember
                        )
                    )
                    onBackClick()
                },
                elevation = 0.dp
            )
        }
    ) { internalPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colorsScheme().background)
                    .padding(internalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VerticalSpace.x24()
            Column(
                modifier =
                    Modifier
                        .padding(horizontal = dimensions().spacing16x)
                        .clip(RoundedCornerShape(dimensions().spacing8x))
                        .fillMaxWidth()
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                        .background(Color.White),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VerticalSpace.x16()
                // Generated QR code
                QrCodeView(
                    data = state.userProfileLink,
                    modifier = Modifier.size(dimensions().spacing300x),
                    dotShape = DotShape.Square
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .border(
                                    width = dimensions().spacing2x,
                                    shape = CircleShape,
                                    color = Color.White
                                )
                                .background(colorsScheme().primary)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            modifier = Modifier.background(color = colorsScheme().primary),
                            tint = Color.White,
                            contentDescription = null
                        )
                    }
                }
                // Handle
                VerticalSpace.x16()
                Text(
                    text = state.handle.ifNotEmpty { "@${state.handle}" },
                    style = MaterialTheme.wireTypography.title01,
                    color = Color.Black
                )

                // Full Link
                VerticalSpace.x16()
                Text(
                    modifier = Modifier.padding(horizontal = dimensions().spacing24x),
                    text = state.userAccountProfileLink,
                    style = MaterialTheme.wireTypography.subline01,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                VerticalSpace.x16()
            }
            Text(
                modifier = Modifier.padding(horizontal = dimensions().spacing24x, vertical = dimensions().spacing16x),
                text = stringResource(id = R.string.user_profile_qr_code_description),
                style = MaterialTheme.wireTypography.body01,
                textAlign = TextAlign.Center,
                color = colorsScheme().secondaryText
            )
            Spacer(modifier = Modifier.weight(1f))
            ShareLinkButton(state.isTeamMember, state.userAccountProfileLink, trackAnalyticsEvent)
            VerticalSpace.x8()
            ShareQRCodeButton {
                trackAnalyticsEvent(
                    AnalyticsEvent.QrCode.Modal.ShareQrCode(
                        isTeam = state.isTeamMember
                    )
                )
                coroutineScope.launch {
                    val bitmap = graphicsLayer.toImageBitmap()
                    val qrUri = shareQRAssetClick(bitmap.asAndroidBitmap())
                    context.shareQRToProfile(qrUri)
                }
            }
        }
    }
}

@SuppressLint("ComposeModifierMissing")
@Composable
fun ShareQRCodeButton(shareQRAssetClick: () -> Unit) {
    WirePrimaryButton(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = dimensions().spacing16x)
                .testTag("Share QR link"),
        text = stringResource(R.string.user_profile_qr_code_share_image_link),
        onClick = shareQRAssetClick
    )
}

@Composable
private fun ShareLinkButton(
    isTeamMember: Boolean,
    selfProfileUrl: String,
    trackAnalyticsEvent: (AnalyticsEvent.QrCode.Modal) -> Unit
) {
    val context = LocalContext.current
    WirePrimaryButton(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = dimensions().spacing16x)
                .testTag("Share link"),
        text = stringResource(R.string.user_profile_qr_code_share_link),
        onClick = {
            trackAnalyticsEvent(
                AnalyticsEvent.QrCode.Modal.ShareProfileLink(
                    isTeam = isTeamMember
                )
            )
            context.shareLinkToProfile(selfProfileUrl)
        }
    )
}

@PreviewMultipleThemes
@Preview(showBackground = true, device = Devices.NEXUS_7_2013)
@Preview(showBackground = true, device = Devices.PIXEL_7A)
@Composable
fun PreviewSelfQRCodeContent() {
    WireTheme {
        SelfQRCodeContent(
            SelfQRCodeState(
                userId = UserId("userId", "wire.com"),
                handle = "userid",
                userProfileLink = "wire://user/wire.com/aaaaaaa-222-3333-4444-55555555",
                userAccountProfileLink = "https://account.wire.com/user-profile/?id=aaaaaaa-222-3333-4444-55555555@wire.com"
            ),
            { "".toUri() },
            { }
        )
    }
}
