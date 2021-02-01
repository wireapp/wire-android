package com.wire.android.shared.asset.di

import com.wire.android.core.network.NetworkClient
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.shared.asset.datasources.remote.AssetApi
import org.koin.dsl.module

val assetModule = module {
    single { get<NetworkClient>().create(AssetApi::class.java) }

    factory { get<UserDatabase>().assetDao() }
}
