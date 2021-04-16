package com.wire.android.shared.asset.mapper

import com.wire.android.shared.asset.datasources.local.AssetEntity
import com.wire.android.shared.asset.datasources.remote.AssetResponse

const val ASSET_SIZE_PROFILE_PICTURE = "complete"

class AssetMapper {

    fun fromResponseToEntity(assetResponse: AssetResponse): AssetEntity =
        AssetEntity(key = assetResponse.key, size = assetResponse.size, type = assetResponse.type)
}

fun profilePictureAssetKey(assets: List<AssetResponse>): String? =
    assets.find { it.size == ASSET_SIZE_PROFILE_PICTURE }?.key
