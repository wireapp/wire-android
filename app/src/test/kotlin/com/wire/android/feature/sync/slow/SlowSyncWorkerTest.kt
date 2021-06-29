package com.wire.android.feature.sync.slow

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.wire.android.UnitTest
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.functional.Either
import com.wire.android.feature.sync.conversation.usecase.RefineConversationNamesUseCase
import com.wire.android.feature.sync.conversation.usecase.SyncAllConversationMembersUseCase
import com.wire.android.feature.sync.conversation.usecase.SyncConversationsUseCase
import com.wire.android.feature.sync.di.syncModule
import com.wire.android.feature.sync.slow.usecase.SetSlowSyncCompletedUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkClass
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.mock.MockProviderRule
import org.koin.test.mock.declareMock

@ExperimentalCoroutinesApi
class SlowSyncWorkerTest : UnitTest(), KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(syncModule)
    }

    @get:Rule
    val mockProvider = MockProviderRule.create { clazz ->
        mockkClass(clazz)
    }

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var workerParams: WorkerParameters

    private lateinit var dispatcherProvider: DispatcherProvider

    private lateinit var syncConversationsUseCase: SyncConversationsUseCase
    private lateinit var syncAllConversationMembersUseCase : SyncAllConversationMembersUseCase
    private lateinit var refineConversationNamesUseCase: RefineConversationNamesUseCase
    private lateinit var setSlowSyncCompletedUseCase: SetSlowSyncCompletedUseCase

    private lateinit var slowSyncWorker: SlowSyncWorker

    @Before
    fun setUp() {
        dispatcherProvider = declareMock { every { io() } returns TestCoroutineDispatcher() }
        syncConversationsUseCase = declareMock()
        syncAllConversationMembersUseCase = declareMock()
        refineConversationNamesUseCase = declareMock()
        setSlowSyncCompletedUseCase = declareMock()

        slowSyncWorker = SlowSyncWorker(context, workerParams)
    }

    @Test
    fun `given doWork is called, when syncConversationsUseCase fails, then returns Result of failure`() {
        coEvery { syncConversationsUseCase.run(Unit) } returns Either.Left(mockk())

        val result = runBlocking { slowSyncWorker.doWork() }

        result shouldBeEqualTo ListenableWorker.Result.failure()
    }

    @Test
    fun `given doWork is called and syncConvsUseCase succeeds, when syncAllConvMembersUseCase fails, then returns Result of failure`() {
        coEvery { syncConversationsUseCase.run(Unit) } returns Either.Right(Unit)
        coEvery { syncAllConversationMembersUseCase.run(Unit) } returns Either.Left(mockk())

        val result = runBlocking { slowSyncWorker.doWork() }

        result shouldBeEqualTo ListenableWorker.Result.failure()
    }

    @Test
    fun `given doWork is called & syncAllConvMembersUseCase succeeds, when refineConvNamesUseCase fails, then returns Result failure`() {
        coEvery { syncConversationsUseCase.run(Unit) } returns Either.Right(Unit)
        coEvery { syncAllConversationMembersUseCase.run(Unit) } returns Either.Right(Unit)
        coEvery { refineConversationNamesUseCase.run(Unit) } returns Either.Left(mockk())

        val result = runBlocking { slowSyncWorker.doWork() }

        result shouldBeEqualTo ListenableWorker.Result.failure()
    }


    @Test
    fun `given doWork is called & refineConvNamesUseCase succeeds, when setSlowSyncCompletedUseCase fails, then returns Result failure`() {
        coEvery { syncConversationsUseCase.run(Unit) } returns Either.Right(Unit)
        coEvery { syncAllConversationMembersUseCase.run(Unit) } returns Either.Right(Unit)
        coEvery { refineConversationNamesUseCase.run(Unit) } returns Either.Right(Unit)
        coEvery { setSlowSyncCompletedUseCase.run(Unit) } returns Either.Left(mockk())

        val result = runBlocking { slowSyncWorker.doWork() }

        result shouldBeEqualTo ListenableWorker.Result.failure()
    }

    @Test
    fun `given doWork is called & refineConvNamesUseCase succeeds, when setSlowSyncCompltdUseCase succeeds, then returns Result success`() {
        coEvery { syncConversationsUseCase.run(Unit) } returns Either.Right(Unit)
        coEvery { syncAllConversationMembersUseCase.run(Unit) } returns Either.Right(Unit)
        coEvery { refineConversationNamesUseCase.run(Unit) } returns Either.Right(Unit)
        coEvery { setSlowSyncCompletedUseCase.run(Unit) } returns Either.Right(Unit)

        val result = runBlocking { slowSyncWorker.doWork() }

        result shouldBeEqualTo ListenableWorker.Result.success()
    }
}
