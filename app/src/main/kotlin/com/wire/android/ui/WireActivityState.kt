package com.wire.android.ui

sealed class WireActivityState {

    data class NavigationGraph(val startNavigationRoute: String, val navigationArguments: List<Any>): WireActivityState()
    data class ClientUpdateRequired(val clientUpdateUrl: String): WireActivityState()
    object ServerVersionNotSupported: WireActivityState()
    object Loading: WireActivityState()
}
