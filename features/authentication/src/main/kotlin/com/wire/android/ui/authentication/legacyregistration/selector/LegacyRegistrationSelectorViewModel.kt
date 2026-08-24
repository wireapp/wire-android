/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration.selector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class LegacyRegistrationSelectorInput<LinksT>(val customServerConfig: LinksT?, val email: String?)
fun interface LegacyRegistrationSelectorGateway { suspend fun setAnonymousRegistrationEnabled(enabled: Boolean) }

/** Separate legacy selector policy: returning from a child disables anonymous-registration tracking. */
open class LegacyRegistrationSelectorViewModel<LinksT>(
    input: LegacyRegistrationSelectorInput<LinksT>,
    defaultServerConfig: LinksT,
    private val gateway: LegacyRegistrationSelectorGateway,
) : ViewModel() {
    val serverConfig: LinksT = input.customServerConfig ?: defaultServerConfig
    val email: String = input.email.orEmpty()
    fun onPageLoaded() = viewModelScope.launch { gateway.setAnonymousRegistrationEnabled(false) }
}
