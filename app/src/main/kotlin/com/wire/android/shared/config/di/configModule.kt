package com.wire.android.shared.config.di

import com.wire.android.shared.config.DeviceTypeMapper
import org.koin.dsl.module

val configMapperModule = module {
    factory { DeviceTypeMapper() }
}
