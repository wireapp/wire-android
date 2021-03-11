package com.wire.android.shared.asset.di

import com.wire.android.core.network.NetworkClient
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.shared.asset.AssetRepository
import com.wire.android.shared.asset.datasources.AssetDataSource
import com.wire.android.shared.asset.datasources.remote.AssetApi
import com.wire.android.shared.asset.datasources.remote.AssetRemoteDataSource
import com.wire.android.shared.asset.ui.imageloader.publicasset.PublicAssetLoaderFactory
import org.koin.dsl.module

val assetModule = module {
    single { get<NetworkClient>().create(AssetApi::class.java) }

    factory { get<UserDatabase>().assetDao() }

    single { AssetRemoteDataSource(get(), get()) }
    single<AssetRepository> { AssetDataSource(get()) }

    single { PublicAssetLoaderFactory(get()) }
}
