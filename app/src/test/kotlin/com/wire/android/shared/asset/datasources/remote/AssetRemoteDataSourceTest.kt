package com.wire.android.shared.asset.datasources.remote

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import com.wire.android.framework.network.mockNetworkError
import com.wire.android.framework.network.mockNetworkResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class AssetRemoteDataSourceTest : UnitTest() {

    @MockK
    private lateinit var assetApi: AssetApi

    private lateinit var assetRemoteDataSource: AssetRemoteDataSource

    @Before
    fun setUp() {
        assetRemoteDataSource = AssetRemoteDataSource(assetApi, connectedNetworkHandler)
    }

    @Test
    fun `given publicAsset is called, when assetApi fails to fetch asset, then propagates failure`() {
        coEvery { assetApi.publicAsset(any()) } returns mockNetworkError()

        val result = runBlocking { assetRemoteDataSource.publicAsset(TEST_ASSET_KEY) }

        result shouldFail {}
        coVerify { assetApi.publicAsset(TEST_ASSET_KEY) }
    }

    @Test
    fun `given publicAsset is called, when assetApi fetches asset, then propagates response`() {
        val responseBody = mockk<ResponseBody>()
        coEvery { assetApi.publicAsset(any()) } returns mockNetworkResponse(responseBody)

        val result = runBlocking { assetRemoteDataSource.publicAsset(TEST_ASSET_KEY) }

        result shouldSucceed { it shouldBeEqualTo responseBody }
        coVerify { assetApi.publicAsset(TEST_ASSET_KEY) }
    }

    companion object {
        private const val TEST_ASSET_KEY = "asset-key-2309"
    }
}
