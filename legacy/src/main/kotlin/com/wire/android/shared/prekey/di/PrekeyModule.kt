package com.wire.android.shared.prekey.di

import com.wire.android.core.network.NetworkClient
import com.wire.android.shared.prekey.PreKeyRepository
import com.wire.android.shared.prekey.data.PreKeyDataSource
import com.wire.android.shared.prekey.data.remote.PreKeyAPI
import com.wire.android.shared.prekey.data.remote.PreKeyRemoteDataSource
import com.wire.android.shared.prekey.data.remote.RemotePreKeyListMapper
import com.wire.android.shared.prekey.data.remote.RemotePreKeyMapper
import org.koin.dsl.module

val prekeyModule = module {
    factory { get<NetworkClient>().create(PreKeyAPI::class.java) }
    factory { RemotePreKeyMapper() }
    factory { RemotePreKeyListMapper(get()) }
    factory { PreKeyRemoteDataSource(get(), get(), get()) }
    single<PreKeyRepository> { PreKeyDataSource(get()) }
}
