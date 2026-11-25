/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.navigation.routes.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.wire.android.navigation.navigation3.wireViewModelStoreOwner
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel
import com.wire.android.ui.authentication.login.mfa.MFALoginMethod
import com.wire.android.ui.authentication.login.mfa.MFALoginMethodScreen
import com.wire.android.ui.authentication.login.mfa.MFALoginRecoveryCodeScreen
import com.wire.android.ui.authentication.login.mfa.MFALoginScreen
import com.wire.android.ui.authentication.login.mfa.RecoveryCodeLoginNotice
import com.wire.android.ui.authentication.loginEmailViewModel
import com.wire.android.ui.newauthentication.login.password.LoginStateNavigationAndDialogs
import com.wire.android.ui.newauthentication.login.password.NewLoginPasswordScreenAction
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import com.wire.navigation.WireViewModelOwner
import kotlinx.coroutines.launch

@Composable
internal fun MFALoginNavigation3Entry(
    route: MFALoginRoute,
    actions: AuthenticationNavigation3Actions,
    router: AuthenticationNavigation3Router,
) {
    val flowOwner = wireViewModelStoreOwner(WireViewModelOwner.Flow(route.flowId))
    val viewModel = loginEmailViewModel(route.args.toLegacy(), flowOwner)
    val scope = rememberCoroutineScope()
    val isCurrentRoute = router.isCurrentRoute(route)
    var isNavigatingBack by remember { mutableStateOf(false) }
    val navigateBack: () -> Unit = {
        val isRecoverySubmitting = route.step == MFALoginStep.RECOVERY_CODE &&
            (viewModel.isVerifyingMFACode || viewModel.loginState.flowState == LoginState.Loading)
        if (!isNavigatingBack && router.isCurrentRoute(route) && !isRecoverySubmitting) {
            isNavigatingBack = true
            scope.launch {
                if (route.step == MFALoginStep.CODE) {
                    viewModel.cancelMFALogin()
                }
                if (route.step == MFALoginStep.RECOVERY_CODE) {
                    viewModel.clearMFARecoveryCode()
                }
                router.backOrElse(actions::exitAuthentication)
            }
        }
    }
    LaunchedEffect(viewModel.hasPendingMFALogin, isCurrentRoute) {
        if (!viewModel.hasPendingMFALogin && isCurrentRoute) {
            navigateBack()
        }
    }
    if (viewModel.hasPendingMFALogin) {
        if (isCurrentRoute) {
            LoginStateNavigationAndDialogs(viewModel) { action ->
                handleMFALoginAction(route, router, action, viewModel.usedRecoveryCodeForLogin, navigateBack)
            }
        }
        MFALoginContent(route, viewModel, router, actions, isNavigatingBack, navigateBack)
    }
}

@Composable
private fun MFALoginContent(
    route: MFALoginRoute,
    viewModel: LoginEmailViewModel,
    router: AuthenticationNavigation3Router,
    actions: AuthenticationNavigation3Actions,
    isNavigatingBack: Boolean,
    onBack: () -> Unit,
) {
    val isBusy = viewModel.isVerifyingMFACode || viewModel.loginState.flowState == LoginState.Loading || isNavigatingBack
    val canSubmit = viewModel.canVerifyMFACode && !isNavigatingBack && router.isCurrentRoute(route)
    when (route.step) {
        MFALoginStep.CODE -> MFALoginScreen(
            code = viewModel.mfaCodeTextState,
            isBusy = isBusy,
            isCodeInvalid = viewModel.isMFACodeInvalid,
            canSubmit = canSubmit,
            onCodeComplete = viewModel::verifyMFACode,
            onBack = onBack,
            onUseAnotherMethod = { openMFALoginMethods(route, viewModel, router) },
            authenticatorName = viewModel.selectedMFAAuthenticator?.name,
        )
        MFALoginStep.METHODS -> MFALoginMethodScreen(
            methods = viewModel.mfaLoginMethods,
            isEnabled = canSubmit && !isBusy,
            onMethodSelected = { selectMFALoginMethod(route, it, viewModel, router, actions) },
            onBack = onBack,
        )
        MFALoginStep.RECOVERY_CODE -> MFALoginRecoveryCodeScreen(
            code = viewModel.mfaRecoveryCodeTextState,
            isBusy = isBusy,
            isCodeInvalid = viewModel.isMFARecoveryCodeInvalid,
            canSubmit = canSubmit && viewModel.isMFARecoveryCodeValid,
            onConfirm = viewModel::verifyMFARecoveryCode,
            onBack = onBack,
        )
    }
}

private fun handleMFALoginAction(
    route: MFALoginRoute,
    router: AuthenticationNavigation3Router,
    action: NewLoginPasswordScreenAction,
    usedRecoveryCode: Boolean,
    onBack: () -> Unit,
): Boolean = when (action) {
    is NewLoginPasswordScreenAction.Success -> router.completeLogin(
        eventId = "${route.entryId.value}:login-terminal",
        completion = when {
            action.isE2EIRequired -> AuthenticationLoginCompletion.E2EIEnrollment(action.userId.toSessionId())
            action.initialSyncCompleted -> AuthenticationLoginCompletion.Home(action.userId.toSessionId())
            else -> AuthenticationLoginCompletion.InitialSync(action.userId.toSessionId())
        },
        recoveryCodeNotice = recoveryCodeLoginNotice(route, action.userId, usedRecoveryCode),
    )
    is NewLoginPasswordScreenAction.RemoveDevice -> router.completeLogin(
        "${route.entryId.value}:login-terminal",
        AuthenticationLoginCompletion.RemoveDevice(action.userId.toSessionId()),
        recoveryCodeNotice = recoveryCodeLoginNotice(route, action.userId, usedRecoveryCode),
    )
    NewLoginPasswordScreenAction.Canceled -> {
        onBack()
        true
    }
    else -> false
}

private fun recoveryCodeLoginNotice(
    route: MFALoginRoute,
    userId: UserId,
    usedRecoveryCode: Boolean,
): RecoveryCodeLoginNotice? = if (usedRecoveryCode) {
    // Temporary metadata for the Android-only recovery-login spike.
    RecoveryCodeLoginNotice(
        sessionId = userId.toSessionId(),
        loginEventId = "${route.entryId.value}:login-terminal",
        remainingCodes = 9,
        totalCodes = 10,
    )
} else {
    null
}

private fun openMFALoginMethods(
    route: MFALoginRoute,
    viewModel: LoginEmailViewModel,
    router: AuthenticationNavigation3Router,
) {
    if (!router.isCurrentRoute(route) || !viewModel.canVerifyMFACode || viewModel.isVerifyingMFACode) return
    router.navigate(
        AuthenticationNavigationTransition.MFA_CODE_TO_METHODS,
        route.copy(step = MFALoginStep.METHODS, entryId = WireNavEntryId.random()),
    )
}

private fun selectMFALoginMethod(
    route: MFALoginRoute,
    method: MFALoginMethod,
    viewModel: LoginEmailViewModel,
    router: AuthenticationNavigation3Router,
    actions: AuthenticationNavigation3Actions,
) {
    if (!router.isCurrentRoute(route) || !viewModel.canVerifyMFACode || viewModel.isVerifyingMFACode) return
    when (method) {
        is MFALoginMethod.Authenticator -> {
            if (viewModel.selectMFAAuthenticator(method)) {
                router.backOrElse(actions::exitAuthentication)
            }
        }
        MFALoginMethod.RecoveryCode -> {
            viewModel.clearMFARecoveryCode()
            router.navigate(
                AuthenticationNavigationTransition.MFA_METHODS_TO_RECOVERY,
                route.copy(step = MFALoginStep.RECOVERY_CODE, entryId = WireNavEntryId.random()),
            )
        }
    }
}

private fun UserId.toSessionId(): WireSessionId = WireSessionId(value = value, domain = domain)
