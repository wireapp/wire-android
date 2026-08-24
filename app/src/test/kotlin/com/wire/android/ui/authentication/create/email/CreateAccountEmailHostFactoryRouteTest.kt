package com.wire.android.ui.authentication.create.email

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.android.navigation.routes.auth.toAuthenticationServerLinks
import com.wire.kalium.logic.configuration.server.ServerConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class CreateAccountEmailHostFactoryRouteTest {
    @Test
    fun `host factory maps route type custom default links and tos`() {
        val arrangement = CreateAccountEmailArrangement(defaultServerConfig = ServerConfig.STAGING)
        val custom = arrangement.hostFactory.create(
            CreateAccountRouteFlowType.TEAM,
            ServerConfig.PRODUCTION.toAuthenticationServerLinks(),
        )
        val fallback = arrangement.hostFactory.create(CreateAccountRouteFlowType.PERSONAL, null)

        assertEquals(CreateAccountRouteFlowType.TEAM, custom.flowType)
        assertEquals(ServerConfig.PRODUCTION, custom.customServerConfig)
        assertEquals(ServerConfig.PRODUCTION, custom.serverConfig)
        assertEquals(ServerConfig.PRODUCTION.tos, custom.tosUrl())
        assertNull(fallback.customServerConfig)
        assertEquals(ServerConfig.STAGING, fallback.serverConfig)
        assertEquals(ServerConfig.STAGING.tos, fallback.tosUrl())
    }
}
