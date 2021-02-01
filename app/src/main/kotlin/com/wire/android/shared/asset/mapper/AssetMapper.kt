package com.wire.android.shared.asset.mapper

import com.wire.android.shared.asset.datasources.local.AssetEntity
import com.wire.android.shared.asset.datasources.remote.AssetResponse

class AssetMapper {

    fun fromResponseToEntity(assetResponse: AssetResponse): AssetEntity =
        AssetEntity(key = assetResponse.key, size = assetResponse.size, type = assetResponse.type)
}
