package com.wire.android.util

import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.configuration.server.ServerConfig

fun newServerConfig(id: Int) = ServerConfig(
    id = "config-$id",
    links = ServerConfig.Links(
        api = "https://server$id-apiBaseUrl.de/",
        accounts = "https://server$id-accountBaseUrl.de/",
        webSocket = "https://server$id-webSocketBaseUrl.de/",
        blackList = "https://server$id-blackListUrl.de/",
        teams = "https://server$id-teamsUrl.de/",
        website = "https://server$id-websiteUrl.de/",
        title = "server$id-title",
        isOnPremises = false
    ),
    metaData = ServerConfig.MetaData(
        commonApiVersion = CommonApiVersionType.Valid(id),
        domain = "domain$id.com",
        federation = false
    )
)
