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

package com.wire.android.ui.home.settings.mfa

import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.ui.home.settings.mfa.authenticatorapps.MFAAuthenticatorAppsEntry
import com.wire.android.ui.home.settings.mfa.authenticatorapps.MFAAuthenticatorAppsRoute
import com.wire.android.ui.home.settings.mfa.authenticatorapps.add.MFAAddAuthenticatorAppEntry
import com.wire.android.ui.home.settings.mfa.authenticatorapps.add.MFAAddAuthenticatorAppRoute
import com.wire.android.ui.home.settings.mfa.authenticatorapps.edit.MFAEditAuthenticatorAppEntry
import com.wire.android.ui.home.settings.mfa.authenticatorapps.edit.MFAEditAuthenticatorAppRoute
import com.wire.android.ui.home.settings.mfa.authenticatorapps.remove.MFARemoveAuthenticatorAppEntry
import com.wire.android.ui.home.settings.mfa.authenticatorapps.remove.MFARemoveAuthenticatorAppRoute
import com.wire.android.ui.home.settings.mfa.overview.MFAOverviewEntry
import com.wire.android.ui.home.settings.mfa.overview.MFAOverviewRoute
import com.wire.android.ui.home.settings.mfa.recoverycodes.MFARecoveryCodesEntry
import com.wire.android.ui.home.settings.mfa.recoverycodes.MFARecoveryCodesRoute
import com.wire.android.ui.home.settings.mfa.setup.MFASetupEntry
import com.wire.android.ui.home.settings.mfa.setup.MFASetupRoute
import com.wire.android.ui.home.settings.mfa.turnoff.MFATurnOffEntry
import com.wire.android.ui.home.settings.mfa.turnoff.MFATurnOffRoute

internal fun mfaNavigation3Entries(
    runtime: WireNavigation3Runtime,
    onExit: () -> Unit,
): WireEntryProviderInstaller = {
    wireEntry<MFAOverviewRoute> { route ->
        MFAOverviewEntry(runtime, route, onExit)
    }
    wireEntry<MFASetupRoute> { route ->
        MFASetupEntry(runtime, route, onExit)
    }
    wireEntry<MFARecoveryCodesRoute> { route ->
        MFARecoveryCodesEntry(runtime, route, onExit)
    }
    wireEntry<MFATurnOffRoute> { route ->
        MFATurnOffEntry(runtime, route, onExit)
    }
    wireEntry<MFAAuthenticatorAppsRoute> { route ->
        MFAAuthenticatorAppsEntry(runtime, route, onExit)
    }
    wireEntry<MFAEditAuthenticatorAppRoute> { route ->
        MFAEditAuthenticatorAppEntry(runtime, route, onExit)
    }
    wireEntry<MFAAddAuthenticatorAppRoute> { route ->
        MFAAddAuthenticatorAppEntry(runtime, route, onExit)
    }
    wireEntry<MFARemoveAuthenticatorAppRoute> { route ->
        MFARemoveAuthenticatorAppEntry(runtime, route, onExit)
    }
}
