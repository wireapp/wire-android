package com.wire.android.ui.authentication.login

import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.user.UserId

typealias AppLoginState = LoginState<CoreFailure, UserId, SSOFailureCodes>
typealias AppLoginDialogError = LoginState.Error.DialogError<CoreFailure, SSOFailureCodes>
