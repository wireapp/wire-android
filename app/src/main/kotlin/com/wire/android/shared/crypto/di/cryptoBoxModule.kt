package com.wire.android.shared.crypto.di

import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.crypto.data.CryptoBoxClientPropertyStorage
import com.wire.android.core.crypto.mapper.PreKeyMapper
import com.wire.android.core.crypto.model.UserId
import com.wire.android.shared.crypto.CryptoBoxRepository
import com.wire.android.shared.crypto.datasources.CryptoBoxDataSource
import com.wire.android.core.device.DeviceTypeUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

const val USER_ID_KOIN_PROPERTY = "user_id"

val cryptoBoxModule = module {
    factory { PreKeyMapper() }
    factory {
        UserId(getProperty(USER_ID_KOIN_PROPERTY))
    }
    factory { CryptoBoxClientPropertyStorage(androidContext()) }
    factory { CryptoBoxClient(androidContext(), get(), get(), get()) }
    factory<CryptoBoxRepository> { CryptoBoxDataSource(get()) }

}
