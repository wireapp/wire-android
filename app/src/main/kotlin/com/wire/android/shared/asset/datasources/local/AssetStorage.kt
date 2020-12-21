package com.wire.android.shared.asset.datasources.local

/**
 * Class that abstracts internal or external storage locations of the app. The actual locations cannot be persisted permanently,
 * since they could differ depending on API versions.
 *
 * @see [AssetEntity.storageType]
 */
sealed class AssetStorage(val type: String) {
    object Internal : AssetStorage("internal")
    object External : AssetStorage("external")
}
