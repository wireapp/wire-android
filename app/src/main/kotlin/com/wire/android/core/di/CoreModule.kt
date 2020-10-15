package com.wire.android.core.di

import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.wire.android.core.accessibility.Accessibility
import com.wire.android.core.accessibility.AccessibilityConfig
import com.wire.android.core.accessibility.InputFocusViewModel
import com.wire.android.core.async.DefaultDispatcherProvider
import com.wire.android.core.async.DispatcherProvider
import com.wire.android.core.compatibility.Compatibility
import com.wire.android.core.locale.LocaleConfig
import com.wire.android.core.ui.dialog.DialogBuilder
import com.wire.android.core.ui.navigation.FragmentStackHandler
import com.wire.android.core.ui.navigation.Navigator
import com.wire.android.core.ui.navigation.UriNavigationHandler
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val accessibilityModule: Module = module {
    factory { androidContext().getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager }
    factory { Accessibility(get()) }
    factory { AccessibilityConfig(get()) }
    viewModel { InputFocusViewModel(get()) }
}

val compatibilityModule: Module = module {
    factory { Compatibility() }
}

val localeModule: Module = module {
    factory { LocaleConfig(androidContext()) }
}

val asyncModule: Module = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
}

val uiModule = module {
    factory { DialogBuilder() }

    single { Navigator(get(), get()) }
    single { FragmentStackHandler() }
    single { UriNavigationHandler() }
}
