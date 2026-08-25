package com.wire.android.navigation.routes.auth

import androidx.compose.runtime.Composable
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.create.common.UserRegistrationInfo
import com.wire.android.ui.authentication.createAccountVerificationCodeViewModel
import com.wire.android.ui.registration.code.CreateAccountVerificationCodeRouteScreen
import com.wire.navigation.WireBackStackMode

@Composable
internal fun createAccountVerificationEntry(
    route: CreateAccountVerificationCodeRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    CreateAccountVerificationCodeRouteScreen(
        viewModel = createAccountVerificationCodeViewModel(route.legacyDataNavArgs(), entryOwner(route.entryId)),
        onNavigateBack = { router.backOrElse(actions::exitAuthentication) },
        onSuccess = { userId ->
            router.navigate(
                AuthenticationNavigationTransition.ACCOUNT_SUMMARY_TO_USERNAME,
                CreateAccountUsernameRoute(userId.toCreateAccountSessionId(), route.flowId),
                WireBackStackMode.CLEAR_WHOLE,
            )
        },
        onTooManyDevices = {
            router.completeLogin(
                route.loginTerminalEventId(),
                AuthenticationLoginCompletion.RemoveDevice(it.toCreateAccountSessionId()),
            )
        },
    )
}

internal fun CreateAccountVerificationCodeRoute.legacyDataNavArgs(): CreateAccountDataNavArgs =
    CreateAccountDataNavArgs(registrationInfo.toLegacyRegistration(), customServerConfig?.toLegacy())

internal fun CreateAccountRegistrationInfo.toLegacyRegistration(): UserRegistrationInfo =
    UserRegistrationInfo(email, name, firstName, lastName, password, teamName, teamIcon)
