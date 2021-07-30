package com.wire.android.shared.prekey.di

import com.wire.android.shared.prekey.PreKeyRepository
import com.wire.android.shared.prekey.data.PreKeyDataSource
import org.koin.dsl.module

val prekeyModule = module {
    single<PreKeyRepository> { PreKeyDataSource(get()) }
}





