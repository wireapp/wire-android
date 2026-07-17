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
package com.wire.android.feature.privacy.auth

import android.content.Context
import androidx.biometric.BiometricManager
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.ApplicationContext
import com.wire.android.util.sha256
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Authentication gateway for highly-sensitive conversations.
 *
 * Capability detection + Chat PIN management live here; the biometric prompt itself is launched from
 * the UI layer (it needs an `AppCompatActivity`) via [com.wire.android.biometric.showBiometricPrompt].
 * Chat PIN is hashed + encrypted using the same primitives as the existing app-lock passcode.
 */
@Inject
class ConversationAuthenticator(
    @ApplicationContext private val context: Context,
    private val globalDataStore: GlobalDataStore,
) {
    /** Whether the device can authenticate via strong biometrics or a device credential (PIN/pattern/passcode). */
    fun canAuthenticateWithDevice(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val strong = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
        val credential = biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) ==
            BiometricManager.BIOMETRIC_SUCCESS
        return strong || credential
    }

    /** Whether the device has strong biometrics (e.g. fingerprint/face) enrolled and ready to use. */
    fun hasEnrolledBiometrics(): Boolean =
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun isChatPinSet(): Flow<Boolean> = globalDataStore.isChatPinSetFlow()

    suspend fun isChatPinSetOnce(): Boolean = isChatPinSet().first()

    suspend fun setChatPin(pin: String) = globalDataStore.setChatPin(pin)

    suspend fun clearChatPin() = globalDataStore.clearChatPin()

    suspend fun verifyChatPin(pin: String): Boolean {
        val storedHash = globalDataStore.getChatPinHashFlow().first() ?: return false
        return storedHash == pin.sha256()
    }

    /**
     * The Chat PIN is the passcode fallback whenever the device has no enrolled biometrics, so when the
     * user assigns HIGHLY_SENSITIVE and no biometrics are enrolled and no Chat PIN exists yet, they must
     * create one so the conversation is never left unprotectable.
     */
    suspend fun requiresChatPinSetup(): Boolean = !hasEnrolledBiometrics() && !isChatPinSetOnce()
}
