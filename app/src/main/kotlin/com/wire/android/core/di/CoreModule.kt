package com.wire.android.core.di

import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.wire.android.core.accessibility.AccessibilityManagerWrapper
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val coreModule: Module = module {
    factory { AccessibilityManagerWrapper(androidContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager) }
}
