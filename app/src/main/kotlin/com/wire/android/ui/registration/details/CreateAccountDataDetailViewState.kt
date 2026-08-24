/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.registration.details

import com.wire.android.ui.authentication.legacyregistration.details.LegacyRegistrationDetailsState
import com.wire.kalium.common.error.CoreFailure

/** Compatibility name for callers while the state owner is :features:authentication. */
typealias CreateAccountDataDetailViewState = LegacyRegistrationDetailsState<CoreFailure>
