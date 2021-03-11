package com.wire.android.shared.asset.ui.imageloader.publicasset

import com.bumptech.glide.Priority
import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.asset.AssetRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.io.InputStream

class PublicAssetFetcherTest : UnitTest() {

    @MockK
    private lateinit var assetRepository: AssetRepository

    private lateinit var publicAssetFetcher: PublicAssetFetcher

    @Before
    fun setUp() {
        publicAssetFetcher = PublicAssetFetcher(assetRepository, TEST_ASSET_KEY)
    }

    @Test
    fun `given fetch is called, when assetRepository fails to fetch public asset, then propagates failure`() {
        val failure = mockk<Failure>()
        coEvery { assetRepository.publicAsset(any()) } returns Either.Left(failure)

        val result = runBlocking { publicAssetFetcher.fetch(Priority.NORMAL) }

        result shouldFail { it shouldBeEqualTo failure }
        coVerify(exactly = 1) { assetRepository.publicAsset(TEST_ASSET_KEY) }
    }

    @Test
    fun `given fetch is called, when assetRepository fetches public asset successfully, then propagates asset stream`() {
        val inputStream = mockk<InputStream>()
        coEvery { assetRepository.publicAsset(any()) } returns Either.Right(inputStream)

        val result = runBlocking { publicAssetFetcher.fetch(Priority.NORMAL) }

        result shouldSucceed { it shouldBeEqualTo inputStream }
        coVerify(exactly = 1) { assetRepository.publicAsset(TEST_ASSET_KEY) }
    }

    companion object {
        private const val TEST_ASSET_KEY = "asset-key-23489"
    }
}
