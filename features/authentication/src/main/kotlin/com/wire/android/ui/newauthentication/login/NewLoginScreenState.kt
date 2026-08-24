/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.newauthentication.login

data class NewLoginScreenState<out LinksT, out FailureT, out SsoFailureT>(
    val isThereActiveSession: Boolean = false,
    val userIdentifierEnabled: Boolean = true,
    val nextEnabled: Boolean = false,
    val flowState: NewLoginFlowState<LinksT, FailureT, SsoFailureT> = NewLoginFlowState.Default,
)
