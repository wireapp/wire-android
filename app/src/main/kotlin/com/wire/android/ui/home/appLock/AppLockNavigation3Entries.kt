/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.appLock

import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.ui.home.appLock.forgot.ForgotLockCodeRouteScreen
import com.wire.android.ui.home.appLock.set.SetLockCodeRouteScreen
import com.wire.android.ui.home.appLock.unlock.AppUnlockWithBiometricsRouteScreen
import com.wire.android.ui.home.appLock.unlock.EnterLockCodeRouteScreen
import com.wire.android.ui.home.settings.appUnlockWithBiometricsViewModel
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireNavigationCommand

internal interface AppLockNavigation3Actions {
    fun completeUnlock()
    fun cancelUnlock()
    fun restartAfterLogout()
}

internal object AppLockNavigation3Contribution {
    val resultTypes = emptyList<com.wire.android.navigation.navigation3.WireNavigation3ResultType<*>>()

    fun entryProviderInstallers(
        runtime: WireNavigation3Runtime,
        actions: AppLockNavigation3Actions,
    ): List<WireEntryProviderInstaller> =
        listOf(appLockNavigation3Entries(runtime, actions))
}

internal fun appLockNavigation3Entries(
    runtime: WireNavigation3Runtime,
    actions: AppLockNavigation3Actions,
): WireEntryProviderInstaller = {
    wireEntry<SetLockCodeRoute> {
        SetLockCodeRouteScreen(
            onBack = {
                if (!runtime.navigator.goBack()) {
                    actions.completeUnlock()
                }
            }
        )
    }
    wireEntry<ForgotLockCodeRoute> {
        ForgotLockCodeRouteScreen(onLogoutCompleted = actions::restartAfterLogout)
    }
    wireEntry<EnterLockCodeRoute> { route ->
        EnterLockCodeRouteScreen(
            onBack = runtime.navigator::goBack,
            canNavigateBackToBiometric = runtime.navigator.routes
                .dropLast(1)
                .lastOrNull() is AppUnlockWithBiometricsRoute,
            onForgotCode = {
                runtime.navigator.navigate(
                    WireNavigationCommand(ForgotLockCodeRoute(route.sessionId))
                )
            },
            onUnlockCompleted = actions::completeUnlock,
        )
    }
    wireEntry<AppUnlockWithBiometricsRoute>(presentation = WireEntryPresentation.None) { route ->
        val viewModel = appUnlockWithBiometricsViewModel()
        AppUnlockWithBiometricsRouteScreen(
            onUnlocked = {
                viewModel.onAppUnlocked()
                actions.completeUnlock()
            },
            onCancel = actions::cancelUnlock,
            onRequestPasscode = {
                runtime.navigator.navigate(
                    WireNavigationCommand(
                        destination = EnterLockCodeRoute(route.sessionId),
                        backStackMode = WireBackStackMode.REMOVE_CURRENT,
                    )
                )
            },
        )
    }
}
