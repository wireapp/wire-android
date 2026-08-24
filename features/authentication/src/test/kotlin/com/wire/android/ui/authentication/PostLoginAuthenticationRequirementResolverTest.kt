package com.wire.android.ui.authentication

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PostLoginAuthenticationRequirementResolverTest {
    @Test
    fun `register device has strict priority over every other post login requirement`() {
        assertEquals(
            PostLoginAuthenticationRequirement.RegisterDevice,
            PostLoginAuthenticationRequirementResolver.resolve(
                needsDeviceRegistration = true,
                initialSyncCompleted = false,
                hasUsername = false,
            ),
        )
    }

    @Test
    fun `initial sync precedes username creation`() {
        assertEquals(
            PostLoginAuthenticationRequirement.InitialSync,
            PostLoginAuthenticationRequirementResolver.resolve(false, false, false),
        )
    }

    @Test
    fun `returns no requirement after completed setup`() {
        assertEquals(null, PostLoginAuthenticationRequirementResolver.resolve(false, true, true))
    }
}
