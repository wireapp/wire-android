package com.wire.android.core.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val coreModule: Module = module {
    factory { androidContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager }
    factory { com.wire.android.core.accessibility.AccessibilityManager(get()) }
}
