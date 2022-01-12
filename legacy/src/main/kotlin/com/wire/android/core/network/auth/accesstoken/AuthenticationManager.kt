package com.wire.android.core.network.auth.accesstoken

import com.wire.android.shared.session.Session

class AuthenticationManager {
    fun authorizationToken(session: Session): String = "${session.tokenType} ${session.accessToken}"
}
