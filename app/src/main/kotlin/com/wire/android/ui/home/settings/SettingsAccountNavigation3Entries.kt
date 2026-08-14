/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.home.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.appLogger
import com.wire.android.ui.home.settings.account.MyAccountRouteScreen
import com.wire.android.ui.home.settings.account.MyAccountUpdateKind
import com.wire.android.ui.home.settings.account.MyAccountUpdateNotification
import com.wire.android.ui.home.settings.account.color.ChangeUserColorRouteScreen
import com.wire.android.ui.home.settings.account.displayname.ChangeDisplayNameRouteScreen
import com.wire.android.ui.home.settings.account.email.updateEmail.ChangeEmailRouteScreen
import com.wire.android.ui.home.settings.account.email.verifyEmail.VerifyEmailRouteScreen
import com.wire.android.ui.home.settings.account.email.verifyEmail.toViewModelArgs
import com.wire.android.ui.home.settings.account.handle.ChangeHandleRouteScreen
import com.wire.android.ui.settings.about.AboutThisAppRouteScreen
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultRequestId
import com.wire.navigation.WireNavigationCommand

internal val SettingsAccountUpdateNavigation3ResultType = WireNavigation3ResultType(
    contract = SettingsAccountUpdateResultContract,
    serializer = SettingsAccountUpdateResult.serializer(),
)

internal fun settingsAccountNavigation3Entries(
    runtime: WireNavigation3Runtime,
    actions: SettingsNavigation3Actions,
): WireEntryProviderInstaller = {
    wireEntry<AboutThisAppRoute> { route ->
        AboutThisAppNavigation3Entry(runtime, route, actions)
    }
    wireEntry<MyAccountRoute> { route ->
        MyAccountNavigation3Entry(runtime, route, actions)
    }
    wireEntry<ChangeEmailRoute>(presentation = WireEntryPresentation.Slide) { route ->
        ChangeEmailNavigation3Entry(runtime, route, actions)
    }
    wireEntry<VerifyEmailRoute> { route ->
        VerifyEmailNavigation3Entry(runtime, route, actions)
    }
    wireEntry<ChangeUserColorRoute>(presentation = WireEntryPresentation.Slide) {
        ChangeUserColorNavigation3Entry(runtime, actions)
    }
    wireEntry<ChangeHandleRoute>(presentation = WireEntryPresentation.Slide) {
        ChangeHandleNavigation3Entry(runtime, actions)
    }
    wireEntry<ChangeDisplayNameRoute>(presentation = WireEntryPresentation.Slide) {
        ChangeDisplayNameNavigation3Entry(runtime, actions)
    }
}

@Composable
private fun AboutThisAppNavigation3Entry(
    runtime: WireNavigation3Runtime,
    route: AboutThisAppRoute,
    actions: SettingsNavigation3Actions,
) {
    AboutThisAppRouteScreen(
        viewModel = com.wire.android.ui.debug.aboutThisAppViewModel(),
        onBackPressed = runtime.backOrExitAccount(actions),
        onItemClicked = { item ->
            item.toNavigation3Route(route.sessionId)?.let { target ->
                runtime.navigator.navigate(WireNavigationCommand(target))
            } ?: actions.open(
                when (item) {
                    SettingsItem.WireWebsite -> SettingsNavigation3Destination.WIRE_WEBSITE
                    SettingsItem.TermsOfUse -> SettingsNavigation3Destination.TERMS_OF_USE
                    SettingsItem.PrivacyPolicy -> SettingsNavigation3Destination.PRIVACY_POLICY
                    else -> error("Unsupported About app item: $item")
                }
            )
        },
    )
}

@Composable
private fun MyAccountNavigation3Entry(
    runtime: WireNavigation3Runtime,
    route: MyAccountRoute,
    actions: SettingsNavigation3Actions,
) {
    var displayNameRequestId by rememberSaveable(route.entryId.value, "display-name") {
        mutableStateOf<String?>(null)
    }
    var handleRequestId by rememberSaveable(route.entryId.value, "handle") {
        mutableStateOf<String?>(null)
    }
    var colorRequestId by rememberSaveable(route.entryId.value, "color") {
        mutableStateOf<String?>(null)
    }
    var updateNotification by remember(route.entryId.value) {
        mutableStateOf<MyAccountUpdateNotification?>(null)
    }

    ConsumeAccountUpdateResult(
        runtime = runtime,
        route = route,
        requestIdValue = displayNameRequestId,
        kind = MyAccountUpdateKind.DISPLAY_NAME,
        onConsumed = { displayNameRequestId = null },
        onNotification = { updateNotification = it },
    )
    ConsumeAccountUpdateResult(
        runtime = runtime,
        route = route,
        requestIdValue = handleRequestId,
        kind = MyAccountUpdateKind.HANDLE,
        onConsumed = { handleRequestId = null },
        onNotification = { updateNotification = it },
    )
    ConsumeAccountUpdateResult(
        runtime = runtime,
        route = route,
        requestIdValue = colorRequestId,
        kind = MyAccountUpdateKind.USER_COLOR,
        onConsumed = { colorRequestId = null },
        onNotification = { updateNotification = it },
    )

    MyAccountRouteScreen(
        viewModel = myAccountViewModel(),
        deleteAccountViewModel = deleteAccountViewModel(),
        onNavigateBack = runtime.backOrExitAccount(actions),
        onChangeDisplayName = {
            displayNameRequestId = runtime.navigateForResult(
                ChangeDisplayNameRoute(route.sessionId),
                SettingsAccountUpdateNavigation3ResultType,
            )?.value
        },
        onChangeHandle = {
            handleRequestId = runtime.navigateForResult(
                ChangeHandleRoute(route.sessionId),
                SettingsAccountUpdateNavigation3ResultType,
            )?.value
        },
        onChangeEmail = {
            runtime.navigator.navigate(WireNavigationCommand(ChangeEmailRoute(route.sessionId)))
        },
        onChangeUserColor = {
            colorRequestId = runtime.navigateForResult(
                ChangeUserColorRoute(route.sessionId),
                SettingsAccountUpdateNavigation3ResultType,
            )?.value
        },
        updateNotification = updateNotification,
    )
}

@Composable
private fun ConsumeAccountUpdateResult(
    runtime: WireNavigation3Runtime,
    route: MyAccountRoute,
    requestIdValue: String?,
    kind: MyAccountUpdateKind,
    onConsumed: () -> Unit,
    onNotification: (MyAccountUpdateNotification) -> Unit,
) {
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    LaunchedEffect(requestIdValue, currentEntryId) {
        if (currentEntryId != route.entryId) return@LaunchedEffect
        val requestId = requestIdValue?.let(::WireNavResultRequestId) ?: return@LaunchedEffect
        when (
            val result = runtime.consumeResult(
                requestId,
                SettingsAccountUpdateNavigation3ResultType,
            )
        ) {
            is WireNavResult.Value -> {
                onNotification(
                    MyAccountUpdateNotification(
                        requestId = requestId.value,
                        kind = kind,
                        successful = result.value.successful,
                    )
                )
                onConsumed()
            }

            WireNavResult.Canceled -> {
                appLogger.i("Error with receiving navigation back args")
                onConsumed()
            }
            null -> Unit
        }
    }
}

@Composable
private fun ChangeEmailNavigation3Entry(
    runtime: WireNavigation3Runtime,
    route: ChangeEmailRoute,
    actions: SettingsNavigation3Actions,
) {
    ChangeEmailRouteScreen(
        viewModel = changeEmailViewModel(),
        onBackPressed = runtime.backOrExitAccount(actions),
        onVerifyEmail = { email ->
            runtime.navigator.navigate(
                WireNavigationCommand(
                    destination = VerifyEmailRoute(route.sessionId, email),
                    backStackMode = WireBackStackMode.REMOVE_CURRENT,
                )
            )
        },
    )
}

@Composable
private fun VerifyEmailNavigation3Entry(
    runtime: WireNavigation3Runtime,
    route: VerifyEmailRoute,
    actions: SettingsNavigation3Actions,
) {
    VerifyEmailRouteScreen(
        viewModel = verifyEmailViewModel(route.toViewModelArgs()),
        onBackPressed = runtime.backOrExitAccount(actions),
    )
}

@Composable
private fun ChangeUserColorNavigation3Entry(
    runtime: WireNavigation3Runtime,
    actions: SettingsNavigation3Actions,
) {
    ChangeUserColorRouteScreen(
        viewModel = changeUserColorViewModel(),
        onBackPressed = runtime.backOrExitAccount(actions),
        onCompleted = { runtime.completeAccountUpdate(it, actions) },
    )
}

@Composable
private fun ChangeHandleNavigation3Entry(
    runtime: WireNavigation3Runtime,
    actions: SettingsNavigation3Actions,
) {
    ChangeHandleRouteScreen(
        viewModel = changeHandleViewModel(),
        onBackPressed = runtime.backOrExitAccount(actions),
        onCompleted = { runtime.completeAccountUpdate(it, actions) },
    )
}

@Composable
private fun ChangeDisplayNameNavigation3Entry(
    runtime: WireNavigation3Runtime,
    actions: SettingsNavigation3Actions,
) {
    ChangeDisplayNameRouteScreen(
        viewModel = changeDisplayNameViewModel(),
        onBackPressed = runtime.backOrExitAccount(actions),
        onCompleted = { runtime.completeAccountUpdate(it, actions) },
    )
}

private fun WireNavigation3Runtime.completeAccountUpdate(
    successful: Boolean,
    actions: SettingsNavigation3Actions,
) {
    if (
        !completeCurrentAndPop(
            SettingsAccountUpdateNavigation3ResultType,
            WireNavResult.Value(SettingsAccountUpdateResult(successful)),
        )
    ) {
        if (!navigator.goBack()) actions.exitSettings()
    }
}

private fun WireNavigation3Runtime.backOrExitAccount(
    actions: SettingsNavigation3Actions,
): () -> Unit = {
    if (!navigator.goBack()) actions.exitSettings()
}
