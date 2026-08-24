/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.welcome

sealed interface WelcomeAction<out LinksT> {
    data class Login<LinksT>(val links: LinksT) : WelcomeAction<LinksT>
    data class OpenUrl(val url: String) : WelcomeAction<Nothing>
    data class CreateTeam<LinksT>(val links: LinksT) : WelcomeAction<LinksT>
    data class CreatePersonal<LinksT>(val links: LinksT) : WelcomeAction<LinksT>
    data class CreateAccountData<LinksT>(val links: LinksT) : WelcomeAction<LinksT>
}

sealed interface WelcomeDialog<out LinksT> {
    data object MaxAccounts : WelcomeDialog<Nothing>
    data object NomadBlocksLogin : WelcomeDialog<Nothing>
    data class TeamBlockedByProxy(val url: String) : WelcomeDialog<Nothing>
    data object PersonalBlockedByProxy : WelcomeDialog<Nothing>
}

data class WelcomePolicy<LinksT>(
    val links: LinksT,
    val proxyEnabled: Boolean,
    val newRegistrationEnabled: Boolean,
    val teamCreationUrl: String,
)

sealed interface WelcomeDecision<out LinksT> {
    data class Action<LinksT>(val value: WelcomeAction<LinksT>) : WelcomeDecision<LinksT>
    data class Dialog<LinksT>(val value: WelcomeDialog<LinksT>) : WelcomeDecision<LinksT>
}

fun <LinksT> welcomeTeamDecision(policy: WelcomePolicy<LinksT>): WelcomeDecision<LinksT> = when {
    policy.proxyEnabled -> WelcomeDecision.Dialog(WelcomeDialog.TeamBlockedByProxy(policy.teamCreationUrl))
    policy.newRegistrationEnabled -> WelcomeDecision.Action(WelcomeAction.OpenUrl(policy.teamCreationUrl))
    else -> WelcomeDecision.Action(WelcomeAction.CreateTeam(policy.links))
}

fun <LinksT> welcomePersonalDecision(policy: WelcomePolicy<LinksT>): WelcomeDecision<LinksT> = when {
    policy.proxyEnabled -> WelcomeDecision.Dialog(WelcomeDialog.PersonalBlockedByProxy)
    policy.newRegistrationEnabled -> WelcomeDecision.Action(WelcomeAction.CreateAccountData(policy.links))
    else -> WelcomeDecision.Action(WelcomeAction.CreatePersonal(policy.links))
}
