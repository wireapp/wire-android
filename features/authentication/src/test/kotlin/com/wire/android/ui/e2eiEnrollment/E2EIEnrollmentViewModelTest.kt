package com.wire.android.ui.e2eiEnrollment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class E2EIEnrollmentViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `certificate outcomes produce feature-owned states`() {
        val viewModel = E2EIEnrollmentViewModel(E2EIEnrollmentGateway {})

        viewModel.enrollE2EICertificate()
        assertTrue(viewModel.state.startGettingE2EICertificate)
        assertTrue(viewModel.state.isLoading)

        viewModel.handleE2EIEnrollmentResult(E2EIEnrollmentResult.Success("certificate"))
        assertEquals("certificate", viewModel.state.certificate)
        assertTrue(viewModel.state.isCertificateEnrollSuccess)
        assertFalse(viewModel.state.isLoading)
    }

    @Test
    fun `finalization awaits the generic gateway before completing`() = runTest(dispatcher) {
        val gatewayEntered = CompletableDeferred<Unit>()
        val releaseGateway = CompletableDeferred<Unit>()
        var complete = false
        val viewModel = E2EIEnrollmentViewModel(
            E2EIEnrollmentGateway {
            gatewayEntered.complete(Unit)
            releaseGateway.await()
        }
        )

        viewModel.finalizeMLSClient { complete = true }
        advanceUntilIdle()

        assertTrue(gatewayEntered.isCompleted)
        assertFalse(complete)
        assertTrue(viewModel.state.isFinalizing)

        releaseGateway.complete(Unit)
        advanceUntilIdle()

        assertTrue(complete)
        assertFalse(viewModel.state.isFinalizing)
    }
}
