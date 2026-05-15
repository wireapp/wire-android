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
package com.wire.ios.shared

import com.wire.ios.shared.auth.login.model.LoginServerLinks

data class WireIosSharedConfig(
    val defaultServerLinks: LoginServerLinks,
    val runtimeConfig: IosKaliumRuntimeConfig? = null,
    val isThereActiveSession: Boolean = false,
    val maxAccountsReached: Boolean = false,
    val nomadAccountBlocksLogin: Boolean = false,
    val isAccountCreationAllowed: Boolean = true,
    val useNewRegistration: Boolean = true,
)

data class IosKaliumRuntimeConfig(
    val appGroupRootPath: String,
    val accountDataPath: String? = null,
    val sqlDelightRootPath: String,
    val coreCryptoPath: String? = null,
    val userId: String? = null,
    val clientId: String? = null,
    val backendDomain: String,
    val serverLinks: LoginServerLinks,
    val migrationMode: MigrationMode,
)

enum class MigrationMode {
    CleanInstallProbe,
    ExistingIosAccountOpenInPlace,
}

fun createWireIosSharedConfig(defaultServerLinks: LoginServerLinks): WireIosSharedConfig =
    WireIosSharedConfig(defaultServerLinks = defaultServerLinks)

fun createWireIosSharedConfig(
    defaultServerLinks: LoginServerLinks,
    runtimeConfig: IosKaliumRuntimeConfig?,
): WireIosSharedConfig =
    WireIosSharedConfig(
        defaultServerLinks = defaultServerLinks,
        runtimeConfig = runtimeConfig,
    )

fun createWireIosSharedConfig(
    defaultServerLinks: LoginServerLinks,
    runtimeConfig: IosKaliumRuntimeConfig?,
    isThereActiveSession: Boolean,
    maxAccountsReached: Boolean,
    nomadAccountBlocksLogin: Boolean,
    isAccountCreationAllowed: Boolean,
    useNewRegistration: Boolean,
): WireIosSharedConfig =
    WireIosSharedConfig(
        defaultServerLinks = defaultServerLinks,
        runtimeConfig = runtimeConfig,
        isThereActiveSession = isThereActiveSession,
        maxAccountsReached = maxAccountsReached,
        nomadAccountBlocksLogin = nomadAccountBlocksLogin,
        isAccountCreationAllowed = isAccountCreationAllowed,
        useNewRegistration = useNewRegistration,
    )

fun createIosKaliumRuntimeConfig(
    appGroupRootPath: String,
    sqlDelightRootPath: String,
    backendDomain: String,
    serverLinks: LoginServerLinks,
    migrationMode: MigrationMode,
): IosKaliumRuntimeConfig =
    IosKaliumRuntimeConfig(
        appGroupRootPath = appGroupRootPath,
        sqlDelightRootPath = sqlDelightRootPath,
        backendDomain = backendDomain,
        serverLinks = serverLinks,
        migrationMode = migrationMode,
    )

fun createIosKaliumRuntimeConfig(
    appGroupRootPath: String,
    accountDataPath: String?,
    sqlDelightRootPath: String,
    coreCryptoPath: String?,
    userId: String?,
    clientId: String?,
    backendDomain: String,
    serverLinks: LoginServerLinks,
    migrationMode: MigrationMode,
): IosKaliumRuntimeConfig =
    IosKaliumRuntimeConfig(
        appGroupRootPath = appGroupRootPath,
        accountDataPath = accountDataPath,
        sqlDelightRootPath = sqlDelightRootPath,
        coreCryptoPath = coreCryptoPath,
        userId = userId,
        clientId = clientId,
        backendDomain = backendDomain,
        serverLinks = serverLinks,
        migrationMode = migrationMode,
    )
