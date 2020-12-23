package com.wire.android.shared.asset.di

import com.wire.android.core.network.NetworkClient
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.shared.asset.datasources.local.AssetLocalDataSource
import com.wire.android.shared.asset.datasources.remote.AssetApi
import com.wire.android.shared.asset.datasources.remote.AssetRemoteDataSource
import org.koin.dsl.module

val assetModule = module {
    single { AssetRemoteDataSource(get(), get()) }
    single { get<NetworkClient>().create(AssetApi::class.java) }

    factory { AssetLocalDataSource(get(), get()) }
    factory { get<UserDatabase>().assetDao() }
}
