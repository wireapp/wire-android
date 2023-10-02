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
 *
 *
 */

package com.wire.android

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

object BiometricPromptUtils {
    private const val TAG = "BiometricPromptUtils"
    fun createBiometricPrompt(
        activity: AppCompatActivity,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationError(errorCode: Int, errorString: CharSequence) {
                super.onAuthenticationError(errorCode, errorString)
                appLogger.i("$TAG errorCode is $errorCode and errorString is: $errorString")
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                appLogger.i("$TAG User biometric rejected")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                appLogger.i("$TAG User biometric accepted")
                onSuccess(result)
            }
        }
        return BiometricPrompt(activity, executor, callback)
    }

    fun createPromptInfo(): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder().apply {
            setTitle("Verify that it's you")
            setSubtitle("Use your fingerprint to continue")
            setConfirmationRequired(true)
            setNegativeButtonText("Cancel")
        }.build()
}

fun AppCompatActivity.showBiometricPrompt(applicationContext: Context) {
    val canAuthenticate = BiometricManager.from(applicationContext)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
        val biometricPrompt = BiometricPromptUtils.createBiometricPrompt(this) { _ ->

        }
        val promptInfo = BiometricPromptUtils.createPromptInfo()
        biometricPrompt.authenticate(promptInfo)
    }
}