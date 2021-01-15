package com.wire.android.shared.asset.datasources.remote

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
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
import retrofit2.Response

class AssetRemoteDataSourceTest : UnitTest() {

    @MockK
    private lateinit var assetApi: AssetApi

    private lateinit var assetRemoteDataSource: AssetRemoteDataSource

    @Before
    fun setUp() {
        assetRemoteDataSource = AssetRemoteDataSource(connectedNetworkHandler, assetApi)
    }

    @Test
    fun `given publicAsset is called, when api call returns response, then propagates success`() {
        val response = mockk<Response<ResponseBody>>()
        val responseBody = mockk<ResponseBody>()
        every { response.isSuccessful } returns true
        every { response.body() } returns responseBody

        coEvery { assetApi.publicAsset(any()) } returns response

        val result = runBlocking { assetRemoteDataSource.publicAsset(TEST_ASSET_KEY) }

        result shouldSucceed { it shouldBeEqualTo responseBody }
        coVerify(exactly = 1) { assetApi.publicAsset(TEST_ASSET_KEY) }
    }

    @Test
    fun `given publicAsset is called, when api call fails, then propagates failure`() {
        val response = mockk<Response<ResponseBody>>()
        every { response.isSuccessful } returns false
        every { response.code() } returns 404

        val result = runBlocking { assetRemoteDataSource.publicAsset(TEST_ASSET_KEY) }

        result shouldFail {}
        coVerify(exactly = 1) { assetApi.publicAsset(TEST_ASSET_KEY) }
    }

    companion object {
        private const val TEST_ASSET_KEY = "asset-key-890"
    }
}
