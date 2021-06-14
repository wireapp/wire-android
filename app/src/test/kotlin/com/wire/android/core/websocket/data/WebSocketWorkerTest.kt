package com.wire.android.core.websocket.data

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.wire.android.UnitTest
import com.wire.android.WireApplication
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.storage.cache.CacheGateway
import com.wire.android.feature.sync.di.syncModule
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkClass
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class WebSocketWorkerTest : UnitTest(), KoinTest {

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
    private lateinit var cacheGateway: CacheGateway
    private lateinit var webSocketConnection: WebSocketConnection
    private lateinit var webSocketWorker: WebSocketWorker

    @Before
    fun setUp() {
        dispatcherProvider = declareMock { every { io() } returns TestCoroutineDispatcher() }
        cacheGateway = declareMock()
        webSocketConnection = declareMock()
        webSocketWorker = WebSocketWorker(context, workerParams)
    }

    @Test
    fun `given doWork is called, when app is in background, then returns Result of failure`() {
        every { cacheGateway.load(WireApplication.IS_IN_BACKGROUND) } returns true

        val result =  webSocketWorker.doWork()

        result shouldBeEqualTo ListenableWorker.Result.failure()
    }

    @Test
    fun `given doWork is called, when websocket connection already established, then returns Result of success`() {
        every { cacheGateway.load(WireApplication.IS_IN_BACKGROUND) } returns false
        every { webSocketConnection.isConnected } returns true

        val result =  webSocketWorker.doWork()

        result shouldBeEqualTo ListenableWorker.Result.success()
    }

    @Test
    fun `given doWork is called, when websocket connection is not established, then establish connection and returns Result of success`() {
        every { cacheGateway.load(WireApplication.IS_IN_BACKGROUND) } returns false
        every { webSocketConnection.isConnected } returns false
        every { webSocketConnection.connect() } returns Unit

        val result =  webSocketWorker.doWork()

        verify(exactly = 1) { webSocketConnection.connect() }
        result shouldBeEqualTo ListenableWorker.Result.success()
    }
}
