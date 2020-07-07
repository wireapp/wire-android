package com.wire.android.core.di

import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.wire.android.core.accessibility.Accessibility
import com.wire.android.core.accessibility.AccessibilityConfig
import com.wire.android.core.network.di.networkModule
import com.wire.android.core.compatibility.Compatibility
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val coreModulesList: List<Module>
    get() = listOf(
        accessibilityModule,
        networkModule,
        compatibilityModule
    )

val accessibilityModule: Module = module {
    factory { androidContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager }
    factory { Accessibility(get()) }
    factory { AccessibilityConfig(get()) }
}

val compatibilityModule: Module = module {
    factory { Compatibility() }
}
