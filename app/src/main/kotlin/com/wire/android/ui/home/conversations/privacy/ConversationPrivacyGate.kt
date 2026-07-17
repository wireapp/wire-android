/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.privacy

import android.os.Build
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.R
import com.wire.android.biometric.showBiometricPrompt
import com.wire.android.feature.privacy.auth.ConversationAuthenticator
import com.wire.android.feature.privacy.session.ConversationAccessState
import com.wire.android.feature.privacy.session.SecureSessionManager
import com.wire.android.ui.common.bottomsheet.WireBottomSheetDefaults
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.topappbar.NavigationIconButton
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.conversationPrivacyGateViewModel
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.wire.android.ui.common.R as commonR

/**
 * Drives the in-conversation lock/blur overlay for the open conversation. Created per conversation
 * screen and backed by the shared [SecureSessionManager], so background / device-lock / inactivity
 * and Panic Mode all flow into [accessState].
 */
class ConversationPrivacyGateViewModel(
    savedStateHandle: SavedStateHandle,
    private val secureSessionManager: SecureSessionManager,
    private val authenticator: ConversationAuthenticator,
) : ViewModel() {

    private val conversationId: ConversationId = savedStateHandle.navArgs<ConversationNavArgs>().conversationId

    val accessState: StateFlow<ConversationAccessState> =
        secureSessionManager.observeAccessState(conversationId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ConversationAccessState.Visible)

    val chatPinSet: StateFlow<Boolean> =
        authenticator.isChatPinSet()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    var pinError by mutableStateOf(false)
        private set

    // Only offer the fingerprint/biometric prompt when biometrics are actually enrolled; otherwise the
    // user authenticates with the Chat PIN passcode.
    fun canUseBiometrics(): Boolean = authenticator.hasEnrolledBiometrics()

    fun onUserActivity() = secureSessionManager.userActivity(conversationId)

    fun onUnlocked() {
        pinError = false
        secureSessionManager.markUnlocked(conversationId)
    }

    fun submitPin(pin: String) {
        viewModelScope.launch {
            if (authenticator.verifyChatPin(pin)) onUnlocked() else pinError = true
        }
    }

    fun clearPinError() {
        pinError = false
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

@Composable
fun ConversationLockGate(
    onBack: () -> Unit,
    viewModel: ConversationPrivacyGateViewModel = conversationPrivacyGateViewModel(),
) {
    val access by viewModel.accessState.collectAsStateWithLifecycle()
    when (access) {
        ConversationAccessState.Concealed -> ConcealOverlay(onReveal = viewModel::onUserActivity)
        ConversationAccessState.Locked, ConversationAccessState.Authenticating -> LockOverlay(viewModel, onBack = onBack)
        ConversationAccessState.Visible -> Unit
    }
}

@Composable
private fun ConcealOverlay(onReveal: () -> Unit) {
    Dialog(onDismissRequest = onReveal, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = colorsScheme().error,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onReveal),
        ) {
            CenteredColumn {
                Icon(
                    painter = painterResource(commonR.drawable.ic_shield_holo),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = stringResource(R.string.conversation_concealed_reveal),
                    style = MaterialTheme.wireTypography.body01,
                    color = colorsScheme().onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = dimensions().spacing16x),
                )
            }
        }
    }
}

@Composable
private fun LockOverlay(viewModel: ConversationPrivacyGateViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val chatPinSet by viewModel.chatPinSet.collectAsStateWithLifecycle()
    val canUseBiometrics = remember { viewModel.canUseBiometrics() }
    var pin by remember { mutableStateOf("") }

    fun authenticateWithBiometrics() {
        (context as? AppCompatActivity)?.showBiometricPrompt(
            onSuccess = { viewModel.onUnlocked() },
            onCancel = {},
            onRequestPasscode = {},
            onTooManyFailedAttempts = {},
        )
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        // Blur the conversation behind the lock (instead of an opaque cover) so the user still sees
        // a frosted preview while the content stays unreadable; unlock actions sit in a bottom sheet.
        BlurContentBehind()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorsScheme().scrim.copy(alpha = SCRIM_OVER_BLUR_ALPHA)),
        ) {
            // Leave the locked conversation without unlocking it.
            NavigationIconButton(
                iconType = NavigationIconType.Back(),
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding(),
            )

            LockBottomSheet(
                chatPinSet = chatPinSet,
                canUseBiometrics = canUseBiometrics,
                pin = pin,
                pinError = viewModel.pinError,
                onPinChange = {
                    pin = it.filter(Char::isDigit).take(MAX_PIN_LENGTH)
                    viewModel.clearPinError()
                },
                onSubmitPin = { viewModel.submitPin(pin) },
                onBiometricUnlock = ::authenticateWithBiometrics,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * Frosts the conversation rendered behind this dialog's window. Cross-window blur needs API 31+ and
 * can be turned off by the system (battery saver, dev settings); when it isn't applied the window dim
 * still keeps the content unreadable.
 */
@Composable
private fun BlurContentBehind() {
    val view = LocalView.current
    val density = LocalDensity.current
    DisposableEffect(Unit) {
        (view.parent as? DialogWindowProvider)?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                attributes = attributes.apply {
                    blurBehindRadius = with(density) { BLUR_BEHIND_RADIUS.roundToPx() }
                }
                setDimAmount(DIM_AMOUNT_WITH_BLUR)
            } else {
                setDimAmount(DIM_AMOUNT_WITHOUT_BLUR)
            }
        }
        onDispose {}
    }
}

@Composable
private fun CenteredColumn(content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensions().spacing24x),
    ) {
        content()
    }
}

@Composable
private fun LockBottomSheet(
    chatPinSet: Boolean,
    canUseBiometrics: Boolean,
    pin: String,
    pinError: Boolean,
    onPinChange: (String) -> Unit,
    onSubmitPin: () -> Unit,
    onBiometricUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clip(WireBottomSheetDefaults.WireBottomSheetShape)
            .background(WireBottomSheetDefaults.WireSheetContainerColor)
            .padding(horizontal = dimensions().spacing16x)
            .padding(top = dimensions().spacing24x, bottom = dimensions().spacing16x)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Icon(
            painter = painterResource(commonR.drawable.ic_shield_holo),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.conversation_locked_title),
            style = MaterialTheme.wireTypography.title02,
            color = colorsScheme().onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = dimensions().spacing16x),
        )
        Text(
            text = stringResource(R.string.conversation_locked_subtitle),
            style = MaterialTheme.wireTypography.body01,
            color = colorsScheme().secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = dimensions().spacing4x),
        )

        // Chat PIN is the default unlock method.
        if (chatPinSet) {
            OutlinedTextField(
                value = pin,
                onValueChange = onPinChange,
                isError = pinError,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensions().spacing16x),
            )
            if (pinError) {
                Text(
                    text = stringResource(R.string.chat_pin_incorrect),
                    color = colorsScheme().error,
                    style = MaterialTheme.wireTypography.body01,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensions().spacing4x),
                )
            }
            WirePrimaryButton(
                onClick = onSubmitPin,
                text = stringResource(R.string.conversation_locked_unlock),
                state = if (pin.isEmpty()) WireButtonState.Disabled else WireButtonState.Default,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensions().spacing8x),
            )
        }

        // Fingerprint/biometrics is an optional shortcut the user can tap when enrolled.
        if (canUseBiometrics) {
            WireSecondaryButton(
                onClick = onBiometricUnlock,
                text = stringResource(R.string.conversation_locked_unlock_with_biometrics),
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_fingerprint),
                        contentDescription = null,
                        modifier = Modifier.size(dimensions().spacing20x),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensions().spacing8x),
            )
        }
    }
}

private const val MAX_PIN_LENGTH = 8

private val BLUR_BEHIND_RADIUS = 24.dp
private const val DIM_AMOUNT_WITH_BLUR = 0.2f
private const val DIM_AMOUNT_WITHOUT_BLUR = 0.6f
private const val SCRIM_OVER_BLUR_ALPHA = 0.2f

@MultipleThemePreviews
@Composable
fun ConversationLockGatePreview() {
    WireTheme {
        ConcealOverlay{}
    }
}

@MultipleThemePreviews
@Composable
fun LockBottomSheetPreview() {
    WireTheme {
        Box(modifier = Modifier.fillMaxSize().background(colorsScheme().background)) {
            LockBottomSheet(
                chatPinSet = true,
                canUseBiometrics = true,
                pin = "",
                pinError = false,
                onPinChange = {},
                onSubmitPin = {},
                onBiometricUnlock = {},
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
