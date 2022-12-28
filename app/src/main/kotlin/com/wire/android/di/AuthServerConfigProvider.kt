package com.wire.android.di

import com.wire.android.BuildConfig
import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthServerConfigProvider @Inject constructor() {
    //todo check with soft logout
    private val serverConfigDefaultLinks = if (BuildConfig.IS_STAGING) ServerConfig.STAGING else ServerConfig.PRODUCTION
    private val _authServer: MutableStateFlow<ServerConfig.Links> = MutableStateFlow(serverConfigDefaultLinks)
    val authServer: StateFlow<ServerConfig.Links> = _authServer

    fun updateAuthServer(serverLinks: ServerConfig.Links) {
        _authServer.value = serverLinks
    }

    fun updateAuthServer(serverConfig: ServerConfig) {
        _authServer.value = serverConfig.links
    }
}
