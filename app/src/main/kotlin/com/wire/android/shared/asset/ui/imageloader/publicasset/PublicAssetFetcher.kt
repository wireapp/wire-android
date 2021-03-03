package com.wire.android.shared.asset.ui.imageloader.publicasset

import com.bumptech.glide.Priority
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.shared.asset.AssetRepository
import com.wire.android.shared.asset.ui.imageloader.CoroutineDataFetcher
import java.io.InputStream

class PublicAssetFetcher(private val assetRepository: AssetRepository, private val key: String) : CoroutineDataFetcher<InputStream>() {

    override suspend fun fetch(priority: Priority): Either<Failure, InputStream> =
        assetRepository.publicAsset(key)

    override fun getDataClass(): Class<InputStream> = InputStream::class.java
}
