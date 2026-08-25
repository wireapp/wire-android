package com.wire.android.ui.authentication

import kotlinx.coroutines.test.runTest
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

    @Test
    fun `lazy resolver short circuits after device registration requirement`() = runTest {
        val evaluations = mutableListOf<String>()

        val result = PostLoginAuthenticationRequirementResolver.resolve(
            needsDeviceRegistration = {
                evaluations += "device"
                true
            },
            initialSyncCompleted = {
                evaluations += "sync"
                true
            },
            hasUsername = {
                evaluations += "username"
                true
            },
        )

        assertEquals(PostLoginAuthenticationRequirement.RegisterDevice, result)
        assertEquals(listOf("device"), evaluations)
    }

    @Test
    fun `lazy resolver short circuits after initial sync requirement`() = runTest {
        val evaluations = mutableListOf<String>()

        val result = PostLoginAuthenticationRequirementResolver.resolve(
            needsDeviceRegistration = {
                evaluations += "device"
                false
            },
            initialSyncCompleted = {
                evaluations += "sync"
                false
            },
            hasUsername = {
                evaluations += "username"
                true
            },
        )

        assertEquals(PostLoginAuthenticationRequirement.InitialSync, result)
        assertEquals(listOf("device", "sync"), evaluations)
    }

    @Test
    fun `lazy resolver evaluates requirements in post login priority order`() = runTest {
        val evaluations = mutableListOf<String>()

        val result = PostLoginAuthenticationRequirementResolver.resolve(
            needsDeviceRegistration = {
                evaluations += "device"
                false
            },
            initialSyncCompleted = {
                evaluations += "sync"
                true
            },
            hasUsername = {
                evaluations += "username"
                false
            },
        )

        assertEquals(PostLoginAuthenticationRequirement.CreateUsername, result)
        assertEquals(listOf("device", "sync", "username"), evaluations)
    }
}
