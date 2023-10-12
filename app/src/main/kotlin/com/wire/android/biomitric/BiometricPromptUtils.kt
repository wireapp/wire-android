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

package com.wire.android.biomitric

import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
import androidx.core.content.ContextCompat
import com.wire.android.appLogger

object BiometricPromptUtils {
    private const val TAG = "BiometricPromptUtils"
    fun createBiometricPrompt(
        activity: AppCompatActivity,
        onSuccess: () -> Unit,
        onCancel: () -> Unit,
        onRequestPasscode: () -> Unit
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationError(errorCode: Int, errorString: CharSequence) {
                super.onAuthenticationError(errorCode, errorString)
                appLogger.i("$TAG errorCode is $errorCode and errorString is: $errorString")
                if (errorCode == ERROR_NEGATIVE_BUTTON) {
                    onRequestPasscode()
                } else {
                    onCancel()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                appLogger.i("$TAG User biometric rejected")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                appLogger.i("$TAG User biometric accepted")
                onSuccess()
            }
        }
        return BiometricPrompt(activity, executor, callback)
    }

    fun createPromptInfo(): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder().apply {
            setTitle("Verify that it's you")
            setSubtitle("To unlock Wire")
            setConfirmationRequired(false)
            setNegativeButtonText("Use passcode")
        }.build()
}

fun AppCompatActivity.showBiometricPrompt(
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    onRequestPasscode: () -> Unit
) {
    val canAuthenticate = BiometricManager.from(this)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
        val biometricPrompt = BiometricPromptUtils.createBiometricPrompt(
            this,
            onSuccess,
            onCancel,
            onRequestPasscode,
        )
        val promptInfo = BiometricPromptUtils.createPromptInfo()
        biometricPrompt.authenticate(promptInfo)
    }
}
