/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.authentication.login

import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.user.UserId

typealias AppLoginState = LoginState<CoreFailure, UserId, SSOFailureCodes>
typealias AppLoginError = LoginState.Error<CoreFailure, UserId, SSOFailureCodes>
typealias AppLoginDialogError = LoginState.Error.DialogError<CoreFailure, SSOFailureCodes>
