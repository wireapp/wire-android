package com.wire.android.shared.asset.ui.imageloader

import com.bumptech.glide.Priority
import com.bumptech.glide.load.data.DataFetcher
import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class CoroutineDataFetcherTest : UnitTest() {

    @MockK
    private lateinit var callback : DataFetcher.DataCallback<Int>

    private lateinit var coroutineDataFetcher: CoroutineDataFetcher<Int>

    @Test
    fun `given loadData is called, when fetch fails, then notifies callback about failure`() {
        coroutineDataFetcher = object : CoroutineDataFetcher<Int>() {

            override suspend fun fetch(priority: Priority): Either<Failure, Int> =
                Either.Left(ServerError)

            override fun getDataClass(): Class<Int> = Int::class.java
        }

        runBlocking {  coroutineDataFetcher.loadData(Priority.NORMAL, callback) }

        verify(exactly = 1) { callback.onLoadFailed(any()) }
    }

    @Test
    fun `given loadData is called, when fetch succeeds, then notifies callback about success`() {
        val result = 2034
        coroutineDataFetcher = object : CoroutineDataFetcher<Int>() {

            override suspend fun fetch(priority: Priority): Either<Failure, Int> =
                Either.Right(result)

            override fun getDataClass(): Class<Int> = Int::class.java
        }

        runBlocking {  coroutineDataFetcher.loadData(Priority.NORMAL, callback) }

        verify(exactly = 1) { callback.onDataReady(result) }
    }

    @Test
    fun `given loadData is called and fetch is in progress, when cancel is called, then cancels fetch operation`() {
        var fetchFinished = false
        coroutineDataFetcher = object : CoroutineDataFetcher<Int>() {

            override suspend fun fetch(priority: Priority): Either<Failure, Int> {
                delay(500)
                fetchFinished = true
                return Either.Right(5)
            }

            override fun getDataClass(): Class<Int> = Int::class.java
        }

        runBlocking {
            coroutineDataFetcher.loadData(Priority.NORMAL, callback)
            delay(100)
            coroutineDataFetcher.cancel()
        }

        fetchFinished shouldBeEqualTo false
    }
}
