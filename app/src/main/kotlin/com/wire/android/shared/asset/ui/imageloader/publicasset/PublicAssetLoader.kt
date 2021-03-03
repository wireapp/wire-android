package com.wire.android.shared.asset.ui.imageloader.publicasset

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.wire.android.shared.asset.AssetRepository
import com.wire.android.shared.asset.PublicAsset
import com.wire.android.shared.asset.ui.imageloader.ImageLoaderKey
import java.io.InputStream

class PublicAssetLoader(private val assetRepository: AssetRepository) : ModelLoader<PublicAsset, InputStream> {

    override fun buildLoadData(model: PublicAsset, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? =
        ModelLoader.LoadData(
            ImageLoaderKey(model.key, width, height, options),
            PublicAssetFetcher(assetRepository, model.key)
        )

    override fun handles(model: PublicAsset): Boolean = true
}

class PublicAssetLoaderFactory(private val assetRepository: AssetRepository) : ModelLoaderFactory<PublicAsset, InputStream> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<PublicAsset, InputStream> =
        PublicAssetLoader(assetRepository)

    @Suppress("EmptyFunctionBlock")
    override fun teardown() {}
}
