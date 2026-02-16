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

import com.wire.android.navigation.annotation.app.WireRootDestination
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.model.lastActiveDescription
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceDialog
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceDialogState
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceError
import com.wire.android.ui.common.CopyButton
import com.wire.android.ui.common.MLSVerificationIcon
import com.wire.android.ui.common.ProteusVerifiedIcon
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSwitch
import com.wire.android.ui.common.button.wirePrimaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.WireTopAppBarTitle
import com.ramcosta.composedestinations.generated.app.destinations.E2EiCertificateDetailsScreenDestination
import com.wire.android.ui.e2eiEnrollment.GetE2EICertificateUI
import com.wire.android.ui.home.E2EISuccessDialog
import com.wire.android.ui.home.E2EIUpdateErrorWithDismissDialog
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.settings.devices.e2ei.E2EICertificateDetails
import com.wire.android.ui.settings.devices.model.DeviceDetailsState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.deviceDateTimeFormat
import com.wire.android.util.dialogErrorStrings
import com.wire.android.util.extension.formatAsFingerPrint
import com.wire.android.util.extension.formatAsString
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.id.QualifiedClientID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.e2ei.Handle
import com.wire.kalium.logic.feature.e2ei.MLSClientE2EIStatus
import com.wire.kalium.logic.feature.e2ei.MLSClientIdentity
import com.wire.kalium.logic.feature.e2ei.MLSCredentialsType
import com.wire.kalium.logic.feature.e2ei.X509Identity
import com.wire.kalium.logic.feature.e2ei.usecase.FinalizeEnrollmentResult
import kotlinx.datetime.Instant

@WireRootDestination(
    navArgs = DeviceDetailsNavArgs::class,
    style = SlideNavigationAnimation::class, // default should be SlideNavigationAnimation
)
@Composable
fun DeviceDetailsScreen(
    navigator: Navigator,
    viewModel: DeviceDetailsViewModel = hiltViewModel()
) {
    when {
        viewModel.state.error is RemoveDeviceError.InitError -> navigator.navigateBack()
        viewModel.state.deviceRemoved -> navigator.navigateBack()
        else -> DeviceDetailsContent(
            state = viewModel.state,
            passwordTextState = viewModel.passwordTextState,
            onDeleteDevice = viewModel::removeDevice,
            onRemoveConfirm = viewModel::onRemoveConfirmed,
            onDialogDismiss = viewModel::onDialogDismissed,
            onErrorDialogDismiss = viewModel::clearDeleteClientError,
            onNavigateBack = navigator::navigateBack,
            onUpdateClientVerification = viewModel::onUpdateVerificationStatus,
            enrollE2eiCertificate = viewModel::enrollE2EICertificate,
            handleE2EIEnrollmentResult = viewModel::handleE2EIEnrollmentResult,
            onNavigateToE2eiCertificateDetailsScreen = {
                navigator.navigate(
                    NavigationCommand(
                        E2EiCertificateDetailsScreenDestination(
                            E2EICertificateDetails.AfterLoginCertificateDetails(it)
                        )
                    )
                )
            },
            onEnrollE2EIErrorDismiss = viewModel::hideEnrollE2EICertificateError,
            onEnrollE2EISuccessDismiss = viewModel::hideEnrollE2EICertificateSuccess,
            onBreakSession = viewModel::breakSession
        )
    }
}

@Suppress("ComplexMethod")
@Composable
fun DeviceDetailsContent(
    state: DeviceDetailsState,
    passwordTextState: TextFieldState,
    handleE2EIEnrollmentResult: (FinalizeEnrollmentResult) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteDevice: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateToE2eiCertificateDetailsScreen: (MLSClientIdentity) -> Unit = {},
    onRemoveConfirm: () -> Unit = {},
    onDialogDismiss: () -> Unit = {},
    onErrorDialogDismiss: () -> Unit = {},
    enrollE2eiCertificate: () -> Unit = {},
    onUpdateClientVerification: (Boolean) -> Unit = {},
    onEnrollE2EIErrorDismiss: () -> Unit = {},
    onEnrollE2EISuccessDismiss: () -> Unit = {},
    onBreakSession: () -> Unit = {}
) {
    val screenState = rememberConversationScreenState()
    WireScaffold(
        modifier = modifier,
        topBar = { DeviceDetailsTopBar(onNavigateBack, state.device, state.isCurrentDevice, state.isE2EIEnabled) },
        bottomBar = {
            Column(
                Modifier
                    .background(MaterialTheme.wireColorScheme.surface)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            ) {
                if (state.canBeRemoved) {
                    Text(
                        text = stringResource(
                            id = if (BuildConfig.WIPE_ON_DEVICE_REMOVAL) {
                                R.string.remove_device_details_description_with_wipe
                            } else {
                                R.string.remove_device_details_description
                            }
                        ),
                        style = MaterialTheme.wireTypography.body01,
                        color = MaterialTheme.wireColorScheme.onBackground,
                        modifier = Modifier.padding(dimensions().spacing16x)
                    )
                    WirePrimaryButton(
                        text = stringResource(R.string.content_description_remove_devices_screen_remove_icon),
                        onClick = onDeleteDevice,
                        colors = wirePrimaryButtonColors().copy(enabled = colorsScheme().error),
                        modifier = Modifier.padding(
                            start = dimensions().spacing16x,
                            end = dimensions().spacing16x,
                            bottom = dimensions().spacing16x
                        )
                    )
                }
            }
        }
    ) { internalPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
                .background(MaterialTheme.wireColorScheme.surface)
        ) {
            state.device.mlsClientIdentity?.let { identity ->
                item {
                    val name = state.mlsCipherSuiteSignature?.let { stringResource(id = R.string.label_mls_signature, it).uppercase() }
                        ?: stringResource(id = R.string.label_mls_no_signature_type)
                    SectionHeader(
                        name = name,
                        modifier = Modifier
                            .background(MaterialTheme.wireColorScheme.background)
                            .fillMaxWidth()
                    )
                    DeviceMLSSignatureItem(identity.thumbprint, screenState::copyMessage)
                    HorizontalDivider(color = MaterialTheme.wireColorScheme.background)
                }
            }

            if (state.isE2EIEnabled && state.isE2eiCertificateDataAvailable) {
                item {
                    EndToEndIdentityCertificateItem(
                        isE2eiCertificateActivated = state.isE2eiCertificateActivated,
                        mlsClientIdentity = state.mlsClientIdentity,
                        isCurrentDevice = state.isCurrentDevice,
                        isLoadingCertificate = state.isLoadingCertificate,
                        enrollE2eiCertificate = { enrollE2eiCertificate() },
                        showCertificate = onNavigateToE2eiCertificateDetailsScreen
                    )
                    HorizontalDivider(color = colorsScheme().background)
                }
            }
            item {
                SectionHeader(
                    name = stringResource(id = R.string.label_proteus_details).uppercase(),
                    modifier = Modifier
                        .background(MaterialTheme.wireColorScheme.background)
                        .fillMaxWidth()
                )
                DeviceIdItem(state, screenState::copyMessage)
                HorizontalDivider(color = MaterialTheme.wireColorScheme.background)
            }

            state.device.registrationTime?.deviceDateTimeFormat()?.let {
                item {
                    DeviceDetailSectionContent(
                        stringResource(id = R.string.label_client_added_time),
                        AnnotatedString(it)
                    )
                    HorizontalDivider(color = MaterialTheme.wireColorScheme.background)
                }
            }

            state.device.lastActiveInWholeWeeks?.let {
                item {
                    DeviceDetailSectionContent(
                        stringResource(id = R.string.label_client_last_active_label),
                        AnnotatedString(state.device.lastActiveDescription() ?: "")
                    )
                    HorizontalDivider(color = MaterialTheme.wireColorScheme.background)
                }
            }

            item {
                DeviceKeyFingerprintItem(state.fingerPrint, screenState::copyMessage)
                HorizontalDivider(color = MaterialTheme.wireColorScheme.background)
            }

            if (!state.isCurrentDevice && state.fingerPrint != null) {
                item {
                    DeviceVerificationItem(
                        state.device.isVerifiedProteus,
                        true,
                        state.isSelfClient,
                        state.userName,
                        onUpdateClientVerification
                    )
                    HorizontalDivider(color = MaterialTheme.wireColorScheme.background)
                }
            }

            if (BuildConfig.DEBUG && !state.isCurrentDevice) {
                item { BreakSessionButton(onBreakSession) }
            }
        }
        if (state.removeDeviceDialogState is RemoveDeviceDialogState.Visible) {
            RemoveDeviceDialog(
                errorState = state.error,
                state = state.removeDeviceDialogState,
                passwordTextState = passwordTextState,
                onDialogDismiss = onDialogDismiss,
                onRemoveConfirm = onRemoveConfirm
            )
            if (state.error is RemoveDeviceError.GenericError) {
                val (title, message) = state.error.coreFailure.dialogErrorStrings(LocalContext.current.resources)

                WireDialog(
                    title = title,
                    text = message,
                    onDismiss = onErrorDialogDismiss,
                    optionButton1Properties = WireDialogButtonProperties(
                        onClick = onErrorDialogDismiss,
                        text = stringResource(id = R.string.label_ok),
                        type = WireDialogButtonType.Primary
                    )
                )
            }
        }

        if (state.isE2EICertificateEnrollError) {
            E2EIUpdateErrorWithDismissDialog(
                isE2EILoading = state.isLoadingCertificate,
                updateCertificate = { enrollE2eiCertificate() },
                onDismiss = onEnrollE2EIErrorDismiss
            )
        }

        if (state.isE2EICertificateEnrollSuccess && state.mlsClientIdentity != null) {
            E2EISuccessDialog(
                openCertificateDetails = { onNavigateToE2eiCertificateDetailsScreen(state.mlsClientIdentity) },
                dismissDialog = onEnrollE2EISuccessDismiss
            )
        }

        if (state.startGettingE2EICertificate) {
            GetE2EICertificateUI(
                enrollmentResultHandler = { handleE2EIEnrollmentResult(it) },
                isNewClient = false
            )
        }
    }
}

@Composable
private fun BreakSessionButton(onBreakSession: () -> Unit) {
    WirePrimaryButton(
        text = stringResource(R.string.debug_settings_break_session),
        onClick = onBreakSession,
        colors = wirePrimaryButtonColors(),
        modifier = Modifier.padding(
            start = dimensions().spacing16x,
            top = dimensions().spacing16x,
            end = dimensions().spacing16x,
            bottom = dimensions().spacing16x
        )
    )
}

@Composable
private fun DeviceDetailsTopBar(
    onNavigateBack: () -> Unit,
    device: Device,
    isCurrentDevice: Boolean,
    shouldShowE2EIInfo: Boolean
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onNavigateBack,
        elevation = 0.dp,
        titleContent = {
            Row {
                WireTopAppBarTitle(
                    title = device.name.asString(),
                    style = MaterialTheme.wireTypography.title01,
                    maxLines = 2
                )

                if (shouldShowE2EIInfo && device.mlsClientIdentity != null) {
                    MLSVerificationIcon(device.mlsClientIdentity.e2eiStatus)
                }

                if (!isCurrentDevice && device.isVerifiedProteus) {
                    ProteusVerifiedIcon(Modifier.align(Alignment.CenterVertically))
                }
            }
        }
    )
}

@Composable
private fun DeviceIdItem(state: DeviceDetailsState, onCopy: (String) -> Unit) {
    DeviceDetailSectionContent(
        sectionTitle = stringResource(id = R.string.label_client_device_id).uppercase(),
        sectionText = AnnotatedString(state.device.clientId.formatAsString()),
        titleTrailingItem = {
            CopyButton(
                onCopyClicked = {
                    state.device.clientId.formatAsString().let { id -> onCopy(id) }
                }
            )
        }
    )
}

@Composable
fun DeviceKeyFingerprintItem(
    clientFingerPrint: String?,
    onCopy: (String) -> Unit
) {
    DeviceDetailSectionContent(
        stringResource(id = R.string.title_device_key_fingerprint),
        sectionText = clientFingerPrint?.formatAsFingerPrint()
            ?: AnnotatedString(stringResource(id = R.string.label_client_key_fingerprint_not_available)),
        enabled = clientFingerPrint != null,
        titleTrailingItem = {
            CopyButton(
                onCopyClicked = {
                    clientFingerPrint?.let { fingerprint -> onCopy(fingerprint) }
                },
                state = if (clientFingerPrint != null) WireButtonState.Default else WireButtonState.Disabled
            )
        }
    )
}

@Composable
fun DeviceMLSSignatureItem(
    mlsThumbprint: String,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {

        DeviceDetailSectionContent(
            stringResource(id = R.string.label_mls_thumbprint),
            sectionText = mlsThumbprint.formatAsFingerPrint(),
            titleTrailingItem = {
                CopyButton(
                    onCopyClicked = { onCopy(mlsThumbprint) },
                    state = WireButtonState.Default
                )
            }
        )
    }
}

@Composable
fun DeviceVerificationItem(
    state: Boolean,
    enabled: Boolean,
    isSelfClient: Boolean,
    userName: String?,
    onStatusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        DeviceDetailSectionContent(
            sectionTitle = stringResource(id = R.string.title_device_key_fingerprint),
            sectionText = AnnotatedString(
                stringResource(
                    id = when (state) {
                        true -> R.string.label_client_verified
                        false -> R.string.label_client_unverified
                    }
                )
            ),
            titleTrailingItem = {
                WireSwitch(
                    checked = state,
                    onCheckedChange = onStatusChange,
                    enabled = enabled
                )
            }
        )
        VerificationDescription(isSelfClient, userName)
    }
}

@Composable
private fun VerificationDescription(
    isSelfClient: Boolean,
    userName: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dimensions().spacing16x,
                end = dimensions().spacing16x,
                bottom = dimensions().spacing16x
            )
    ) {

        if (isSelfClient) {
            DescriptionText(
                text = stringResource(id = R.string.label_self_client_verification_description),
                leanMoreLink = stringResource(id = R.string.url_self_client_verification_learn_more)
            )
        } else {
            DescriptionText(
                text = stringResource(
                    id = R.string.label_client_verification_description,
                    userName ?: stringResource(id = R.string.unknown_user_name)
                ),
                leanMoreLink = stringResource(id = R.string.url_self_client_verification_learn_more)
            )
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        )

        if (isSelfClient) {
            DescriptionText(
                text = stringResource(id = R.string.label_self_fingerprint_description),
                leanMoreLink = stringResource(id = R.string.url_self_client_fingerprint_learn_more)
            )
        } else {
            DescriptionText(
                text = stringResource(id = R.string.label_fingerprint_description),
                leanMoreLink = null
            )
        }
    }
}

@Composable
private fun DescriptionText(
    text: String,
    leanMoreLink: String?,
) {

    val context = LocalContext.current
    buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = MaterialTheme.wireColorScheme.secondaryText,
                fontWeight = MaterialTheme.wireTypography.body01.fontWeight,
                fontSize = MaterialTheme.wireTypography.body01.fontSize
            )
        ) {
            append(text)
        }

        leanMoreLink?.let {
            pushStringAnnotation(
                tag = "learn_more",
                annotation = it
            )
            append(" ")
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.wireColorScheme.onBackground,
                    fontWeight = MaterialTheme.wireTypography.label05.fontWeight,
                    fontSize = MaterialTheme.wireTypography.label05.fontSize,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(stringResource(id = R.string.label_learn_more))
            }
        }
    }.let { annotatedString ->

        ClickableText(text = annotatedString, onClick = { offset ->
            leanMoreLink?.let {
                annotatedString.getStringAnnotations(
                    tag = "learn_more",
                    start = offset,
                    end = offset
                ).firstOrNull()?.let {
                    CustomTabsHelper.launchUrl(context, it.item)
                }
            }
        })
    }
}

@Composable
private fun DeviceDetailSectionContent(
    sectionTitle: String,
    sectionText: AnnotatedString,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    titleTrailingItem: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(
                top = MaterialTheme.wireDimensions.spacing12x,
                bottom = MaterialTheme.wireDimensions.spacing12x,
                start = MaterialTheme.wireDimensions.spacing16x,
                end = MaterialTheme.wireDimensions.spacing12x
            )
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = sectionTitle,
                style = MaterialTheme.wireTypography.label01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.spacing4x)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sectionText,
                    style = MaterialTheme.wireTypography.body01,
                    color = if (enabled) {
                        MaterialTheme.wireColorScheme.onBackground
                    } else {
                        MaterialTheme.wireColorScheme.secondaryText
                    },
                    modifier = Modifier.weight(weight = 1f, fill = true)
                )

                if (titleTrailingItem != null) {
                    Box(modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.spacing8x)) { titleTrailingItem() }
                }
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewDeviceDetailsScreen() = WireTheme {
    DeviceDetailsContent(
        passwordTextState = TextFieldState(),
        state = DeviceDetailsState(
            device = Device(
                clientId = ClientId(""),
                name = UIText.DynamicString("My Device"),
                registrationTime = "2022-03-24T18:02:30.360Z",
                mlsClientIdentity = MLSClientIdentity(
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
                ),
            ),
            isCurrentDevice = false
        ),
        enrollE2eiCertificate = {},
        handleE2EIEnrollmentResult = {},
        onRemoveConfirm = {},
        onDialogDismiss = {},
        onErrorDialogDismiss = {},
        onNavigateBack = {},
        onNavigateToE2eiCertificateDetailsScreen = {},
        onUpdateClientVerification = {},
        onEnrollE2EIErrorDismiss = {},
        onEnrollE2EISuccessDismiss = {},
        onDeleteDevice = {},
    )
}
