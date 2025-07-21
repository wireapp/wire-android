/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.ramcosta.composedestinations.utils.destination
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.LoginTypeSelector
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.getBaseRoute
import com.wire.android.ui.authentication.login.PreFilledUserIdentifierType
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.ImportMediaScreenDestination
import com.wire.android.ui.destinations.LoginScreenDestination
import com.wire.android.ui.destinations.NewLoginScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import kotlinx.coroutines.flow.Flow

@Composable
internal fun HandleViewActions(actions: Flow<WireActivityViewAction>, navigator: Navigator, loginTypeSelector: LoginTypeSelector) {

    val context = LocalContext.current
    HandleActions(actions) { action ->
        when (action) {
            is OnAuthorizationNeeded -> onAuthorizationNeeded(context, navigator)
            is OnMigrationLogin -> onMigration(navigator, loginTypeSelector, action)
            is OnOpenUserProfile -> openUserProfile(action, navigator)
            is OnSSOLogin -> openSsoLogin(navigator, action)
            is OnShowImportMediaScreen -> openImportMediaScreen(navigator)
            is OpenConversation -> openConversation(action, navigator)
            is OnUnknownDeepLink -> if (navigator.isEmptyWelcomeStartDestination()) {
                // log in needed so if "welcome empty start" screen then switch "start" screen to login by navigating to it
                navigator.navigate(NavigationCommand(NewLoginScreenDestination(), BackStackMode.CLEAR_WHOLE))
            }

            is ShowToast -> showToast(context, action.messageResId)
        }
    }
}

private fun openConversation(action: OpenConversation, navigator: Navigator) {
    if (action.result.switchedAccount) {
        navigator.navigate(
            NavigationCommand(
                HomeScreenDestination,
                BackStackMode.CLEAR_WHOLE
            )
        )
    }
    navigator.navigate(
        NavigationCommand(
            ConversationScreenDestination(action.result.conversationId),
            BackStackMode.UPDATE_EXISTED
        )
    )
}

private fun openImportMediaScreen(navigator: Navigator) {
    navigator.navigate(
        NavigationCommand(
            ImportMediaScreenDestination,
            BackStackMode.UPDATE_EXISTED
        )
    )
}

private fun openSsoLogin(navigator: Navigator, action: OnSSOLogin) {
    navigator.navigate(
        NavigationCommand(
            when (navigator.navController.currentBackStackEntry?.destination()?.route?.getBaseRoute()) {
                // if SSO login started from new login screen then go back to the new login flow
                NewLoginScreenDestination.route.getBaseRoute() -> NewLoginScreenDestination(
                    ssoLoginResult = action.result
                )

                else -> LoginScreenDestination(
                    ssoLoginResult = action.result
                )
            },
            BackStackMode.UPDATE_EXISTED,
        )
    )
}

private fun openUserProfile(action: OnOpenUserProfile, navigator: Navigator) {
    if (action.result.switchedAccount) {
        navigator.navigate(
            NavigationCommand(
                HomeScreenDestination,
                BackStackMode.CLEAR_WHOLE
            )
        )
    }
    navigator.navigate(
        NavigationCommand(
            OtherUserProfileScreenDestination(action.result.userId),
            BackStackMode.UPDATE_EXISTED
        )
    )
}

private fun onMigration(
    navigator: Navigator,
    loginTypeSelector: LoginTypeSelector,
    action: OnMigrationLogin
) {
    navigator.navigate(
        NavigationCommand(
            when (loginTypeSelector.canUseNewLogin()) {
                true -> NewLoginScreenDestination(
                    userHandle = PreFilledUserIdentifierType.PreFilled(action.result.userHandle)
                )

                false -> LoginScreenDestination(
                    userHandle = PreFilledUserIdentifierType.PreFilled(action.result.userHandle)
                )
            },
            // if "welcome empty start" screen then switch "start" screen to proper one
            when (navigator.shouldReplaceWelcomeLoginStartDestination()) {
                true -> BackStackMode.CLEAR_WHOLE
                false -> BackStackMode.UPDATE_EXISTED
            },
        )
    )
}

private fun onAuthorizationNeeded(context: Context, navigator: Navigator) {
    if (navigator.isEmptyWelcomeStartDestination()) {
        // log in needed so if "welcome empty start" screen then switch "start" screen to login by navigating to it
        navigator.navigate(NavigationCommand(NewLoginScreenDestination(), BackStackMode.CLEAR_WHOLE))
    }
    showToast(context, R.string.deeplink_authorization_needed)
}

private fun showToast(context: Context, messageResId: Int) {
    Toast.makeText(
        context,
        context.resources.getString(messageResId),
        Toast.LENGTH_SHORT
    ).show()
}
