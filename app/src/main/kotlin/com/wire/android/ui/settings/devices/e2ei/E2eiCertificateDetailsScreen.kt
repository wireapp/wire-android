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
package com.wire.android.ui.settings.devices.e2ei

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.util.copyLinkToClipboard
import com.wire.android.util.createPemFile
import com.wire.android.util.saveFileToDownloadsFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toOkioPath

@RootNavGraph
@Destination(
    navArgsDelegate = E2eiCertificateDetailsScreenNavArgs::class,
    style = PopUpNavigationAnimation::class,
)
@Composable
fun E2eiCertificateDetailsScreen(
    e2eiCertificateDetailsViewModel: E2eiCertificateDetailsViewModel = hiltViewModel(),
    navigator: Navigator
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = navigator::navigateBack,
                title = stringResource(R.string.e2ei_certificate_details_screen_title),
                navigationIconType = NavigationIconType.Back,
                actions = {
                    WireSecondaryIconButton(
                        onButtonClicked = {
                            e2eiCertificateDetailsViewModel.state.wireModalSheetState.show()
                        },
                        iconResource = R.drawable.ic_more,
                        contentDescription = R.string.content_description_more_options
                    )
                }
            )
        }
    ) {
        val clipboardManager = LocalClipboardManager.current

        with(e2eiCertificateDetailsViewModel) {
            val copiedToClipboardString =
                stringResource(id = R.string.e2ei_certificate_details_certificate_copied_to_clipboard)
            val downloadedString = stringResource(id = R.string.media_gallery_on_image_downloaded)

            E2eiCertificateDetailsContent(
                padding = it,
                certificateString = getCertificate()
            )
            E2eiCertificateDetailsBottomSheet(
                sheetState = state.wireModalSheetState,
                onCopyToClipboard = {
                    clipboardManager.copyLinkToClipboard(getCertificate())
                    scope.launch {
                        state.wireModalSheetState.hide()
                        snackbarHostState.showSnackbar(copiedToClipboardString)
                    }
                },
                onDownload = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            createPemFile(CERTIFICATE_FILE_NAME, getCertificate()).also {
                                saveFileToDownloadsFolder(
                                    context = context,
                                    assetName = CERTIFICATE_FILE_NAME,
                                    assetDataPath = it.toPath().toOkioPath(),
                                    assetDataSize = it.length()
                                )
                            }
                        }
                        state.wireModalSheetState.hide()
                        snackbarHostState.showSnackbar(downloadedString)
                    }
                }
            )
        }
    }
}

@Composable
fun E2eiCertificateDetailsContent(
    padding: PaddingValues,
    certificateString: String
) {
    val textStyle = TextStyle(
        textAlign = TextAlign.Justify,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = colorsScheme().onBackground
    )
    val scroll = rememberScrollState(0)
    Text(
        modifier = Modifier
            .verticalScroll(scroll)
            .padding(
                top = padding.calculateTopPadding() + dimensions().spacing16x,
                start = padding.calculateStartPadding(LayoutDirection.Ltr) + dimensions().spacing16x,
                end = padding.calculateEndPadding(LayoutDirection.Ltr) + dimensions().spacing16x,
                bottom = padding.calculateBottomPadding()
            ),
        text = certificateString,
        style = textStyle
    )
}

const val CERTIFICATE_FILE_NAME = "certificate.pem"
