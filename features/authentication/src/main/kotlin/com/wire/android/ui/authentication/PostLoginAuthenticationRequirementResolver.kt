/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication

/** Pure post-login policy; callers own persistence reads and navigation side effects. */
object PostLoginAuthenticationRequirementResolver {
    fun resolve(
        needsDeviceRegistration: Boolean,
        initialSyncCompleted: Boolean,
        hasUsername: Boolean,
    ): PostLoginAuthenticationRequirement? = when {
        needsDeviceRegistration -> PostLoginAuthenticationRequirement.RegisterDevice
        !initialSyncCompleted -> PostLoginAuthenticationRequirement.InitialSync
        !hasUsername -> PostLoginAuthenticationRequirement.CreateUsername
        else -> null
    }
}

sealed interface PostLoginAuthenticationRequirement {
    data object RegisterDevice : PostLoginAuthenticationRequirement
    data object InitialSync : PostLoginAuthenticationRequirement
    data object CreateUsername : PostLoginAuthenticationRequirement
}
