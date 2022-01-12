package com.wire.android.shared.asset.ui.imageloader.publicasset

import com.wire.android.UnitTest
import com.wire.android.shared.asset.AssetRepository
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class PublicAssetLoaderFactoryTest : UnitTest() {

    @MockK
    private lateinit var assetRepository: AssetRepository

    private lateinit var publicAssetLoaderFactory: PublicAssetLoaderFactory

    @Before
    fun setUp() {
        publicAssetLoaderFactory = PublicAssetLoaderFactory(assetRepository)
    }

    @Test
    fun `given build is called, then returns a new instance of PublicAssetLoader`() {
        val result = publicAssetLoaderFactory.build(mockk())

        result shouldBeInstanceOf PublicAssetLoader::class
    }
}
