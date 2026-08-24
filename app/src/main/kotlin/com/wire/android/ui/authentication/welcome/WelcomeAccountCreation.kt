/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.welcome

import com.wire.android.BuildConfig.ENABLE_NEW_REGISTRATION
import com.wire.android.R
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogState
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.kalium.logic.configuration.server.ServerConfig

internal fun ServerConfig.Links.createTeam(
    url: String,
    dialog: VisibilityState<FeatureDisabledWithProxyDialogState>,
    onAction: (WelcomeScreenAction) -> Unit,
) {
    if (apiProxy != null) {
        dialog.show(
            dialog.savedState ?: FeatureDisabledWithProxyDialogState(
                R.string.create_team_not_supported_dialog_description,
                teams,
            ),
        )
    } else if (ENABLE_NEW_REGISTRATION) {
        onAction(WelcomeScreenAction.OpenUrl(url))
    } else {
        onAction(WelcomeScreenAction.CreateTeam(this))
    }
}

internal fun ServerConfig.Links.createPersonal(
    dialog: VisibilityState<FeatureDisabledWithProxyDialogState>,
    onAction: (WelcomeScreenAction) -> Unit,
) {
    if (apiProxy != null) {
        dialog.show(
            dialog.savedState ?: FeatureDisabledWithProxyDialogState(
                R.string.create_personal_account_not_supported_dialog_description,
            ),
        )
    } else if (ENABLE_NEW_REGISTRATION) {
        onAction(WelcomeScreenAction.CreateAccountData(this))
    } else {
        onAction(WelcomeScreenAction.CreatePersonal(this))
    }
}
