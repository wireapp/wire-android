/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.registration.code

import com.wire.android.BuildConfig
import com.wire.kalium.logic.data.session.StoreSessionParam
import com.wire.kalium.logic.feature.register.RegisterResult

internal fun RegisterResult.Success.toStoreSessionParam(webSocketEnabled: Boolean) = StoreSessionParam(
    accountTokens = authData,
    ssoId = ssoID,
    serverConfigId = serverConfigId,
    proxyCredentials = proxyCredentials,
    isPersistentWebSocketEnabled = webSocketEnabled,
)

internal fun privateBuildModelPostfix(): String? = if (BuildConfig.PRIVATE_BUILD) {
    " [${BuildConfig.FLAVOR}_${BuildConfig.BUILD_TYPE}]"
} else {
    null
}
