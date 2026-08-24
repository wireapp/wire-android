package com.wire.android.navigation.routes.auth

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.authentication.create.summary.CreateAccountSummaryRouteScreen
import com.wire.android.ui.authentication.create.username.CreateAccountUsernameRouteScreen
import com.wire.android.ui.authentication.createAccountUsernameViewModel
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireViewModelOwner

@Composable
internal fun createAccountSummaryCompletionEntry(
    route: CreateAccountSummaryRoute,
    router: AuthenticationNavigation3Router,
) {
    CreateAccountSummaryRouteScreen(
        type = route.type,
        onContinue = {
            router.navigate(
                AuthenticationNavigationTransition.ACCOUNT_SUMMARY_TO_USERNAME,
                CreateAccountUsernameRoute(route.sessionId, route.flowId),
                WireBackStackMode.CLEAR_WHOLE,
            )
        },
    )
}

@Composable
internal fun createAccountUsernameCompletionEntry(
    route: CreateAccountUsernameRoute,
    router: AuthenticationNavigation3Router,
) {
    CreateAccountUsernameRouteScreen(
        viewModel = createAccountUsernameViewModel(entryOwner(route.entryId)),
        onSuccess = {
            router.completeLogin(
                route.loginTerminalEventId(),
                AuthenticationLoginCompletion.InitialSync(route.sessionId),
            )
        },
    )
}

@Composable
internal fun entryOwner(entryId: com.wire.navigation.WireNavEntryId): ViewModelStoreOwner =
    wireViewModelStoreOwner(WireViewModelOwner.Entry(entryId))

internal fun com.wire.navigation.WireRoute.loginTerminalEventId(): String = "${entryId.value}:login-terminal"

internal fun com.wire.kalium.logic.data.user.UserId.toCreateAccountSessionId() =
    com.wire.navigation.WireSessionId(value, domain)
