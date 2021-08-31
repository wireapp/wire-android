package com.wire.android.core.storage.cache.di

import com.wire.android.core.storage.cache.InMemoryCache
import org.koin.dsl.module

val cacheModule = module {
    single { InMemoryCache() }
}
