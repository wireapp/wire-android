/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.settings.devices.e2ei

/**
 * The certificate details actually consumed by the screen ViewModel.
 *
 * The legacy destination may carry a full MLS identity, while Navigation 3 persists only these
 * stable strings. Keeping that conversion outside the ViewModel makes its ownership independent
 * of either navigation runtime.
 */
sealed interface E2eiCertificateDetailsViewModelArgs {
    val certificate: String

    data class DuringLogin(
        override val certificate: String,
    ) : E2eiCertificateDetailsViewModelArgs

    data class AfterLogin(
        override val certificate: String,
        val userHandle: String,
    ) : E2eiCertificateDetailsViewModelArgs
}

internal fun E2eiCertificateDetailsRoute.toViewModelArgs(): E2eiCertificateDetailsViewModelArgs =
    when (val details = details) {
        is E2eiCertificateDetailsPayload.DuringLogin ->
            E2eiCertificateDetailsViewModelArgs.DuringLogin(details.certificate)

        is E2eiCertificateDetailsPayload.AfterLogin ->
            E2eiCertificateDetailsViewModelArgs.AfterLogin(
                certificate = details.certificate,
                userHandle = details.userHandle,
            )
    }
