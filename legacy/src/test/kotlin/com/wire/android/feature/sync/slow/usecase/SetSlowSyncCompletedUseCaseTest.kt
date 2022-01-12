package com.wire.android.feature.sync.slow.usecase

import com.wire.android.UnitTest
import com.wire.android.feature.sync.SyncRepository
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class SetSlowSyncCompletedUseCaseTest : UnitTest() {

    @MockK
    private lateinit var syncRepository: SyncRepository

    private lateinit var setSlowSyncCompletedUseCase: SetSlowSyncCompletedUseCase

    @Before
    fun setUp() {
        setSlowSyncCompletedUseCase = SetSlowSyncCompletedUseCase(syncRepository)
    }

    @Test
    fun `given run is called, when syncRepository successfully sets slow sync completed, then propagates success`() {
        every { syncRepository.setSlowSyncCompleted() } returns Unit

        val result = runBlocking { setSlowSyncCompletedUseCase.run(Unit) }

        result shouldSucceed { it shouldBe Unit }
    }

}
