package com.wire.android.shared.asset.datasources

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.asset.datasources.remote.AssetRemoteDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.io.InputStream

class AssetDataSourceTest : UnitTest() {

    @MockK
    private lateinit var assetRemoteDataSource: AssetRemoteDataSource

    private lateinit var assetDataSource: AssetDataSource

    @Before
    fun setUp() {
        assetDataSource = AssetDataSource(assetRemoteDataSource)
    }

    @Test
    fun `given publicAsset is called, when remoteDataSource fails to fetch asset, then propagates the failure`() {
        val failure = mockk<Failure>()
        coEvery { assetRemoteDataSource.publicAsset(any()) } returns Either.Left(failure)

        val result = runBlocking { assetDataSource.publicAsset(TEST_ASSET_KEY) }

        result shouldFail { it shouldBeEqualTo failure }
        coVerify(exactly = 1) { assetRemoteDataSource.publicAsset(TEST_ASSET_KEY) }
    }

    @Test
    fun `given publicAsset is called, when remoteDataSource fetches asset, then propagates the response byteStream`() {
        val responseBody = mockk<ResponseBody>()
        val inputStream = mockk<InputStream>()
        every { responseBody.byteStream() } returns inputStream
        coEvery { assetRemoteDataSource.publicAsset(any()) } returns Either.Right(responseBody)

        val result = runBlocking { assetDataSource.publicAsset(TEST_ASSET_KEY) }

        result shouldSucceed { it shouldBeEqualTo inputStream }
        coVerify(exactly = 1) { assetRemoteDataSource.publicAsset(TEST_ASSET_KEY) }
    }

    companion object {
        private const val TEST_ASSET_KEY = "asset-key-2309"
    }
}
