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
package com.wire.android.ui.debug.securityproviders

import androidx.annotation.StringRes
import com.wire.android.R
import com.wire.android.datastore.EncryptionManager
import com.wire.android.feature.e2ei.OAuthUseCase
import com.wire.android.util.crypto.AppCryptoServiceRegistry
import com.wire.android.util.crypto.AppCryptoUsage
import dev.zacsweers.metro.Inject

/**
 * Reports which security provider served each cryptographic call site in the app module.
 *
 * Kalium's own call sites come from `GetCryptoServiceReportUseCase`; this covers the ones the app makes
 * directly, which kalium cannot see. Call sites that have not run yet are probed by performing the very
 * same lookup they perform, so every row is a provider the platform actually handed back.
 */
class AppCryptoServicesProvider @Inject constructor() {

    operator fun invoke(): List<CryptoServiceRow> {
        // Neither path runs on app start, so resolve them the same way the call sites do.
        EncryptionManager.probeCryptoServices()
        OAuthUseCase.probeCryptoServices()
        return AppCryptoServiceRegistry.recorded().map { (usage, record) ->
            CryptoServiceRow(
                labelRes = usage.labelRes(),
                lookup = record.lookup,
                algorithm = record.algorithm,
                providerName = record.providerName,
                providerVersion = record.providerVersion,
            )
        }
    }

    @StringRes
    private fun AppCryptoUsage.labelRes(): Int = when (this) {
        AppCryptoUsage.DATASTORE_KEYSTORE -> R.string.debug_settings_crypto_datastore_keystore
        AppCryptoUsage.DATASTORE_KEY_GENERATION -> R.string.debug_settings_crypto_datastore_key_generator
        AppCryptoUsage.DATASTORE_CIPHER -> R.string.debug_settings_crypto_datastore_cipher
        AppCryptoUsage.OAUTH_PKCE_VERIFIER -> R.string.debug_settings_crypto_oauth_verifier
        AppCryptoUsage.OAUTH_PKCE_CHALLENGE -> R.string.debug_settings_crypto_oauth_challenge
    }
}
