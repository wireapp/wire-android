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
package com.wire.android.tests.core.e2eTests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColumnEnvironmentTest {

    @TestCaseId("TC-8710")
    @Category("column", "links")
    @Ignore(
        "Blocked: column settings link verification needs column backend login, email 2FA, About/Legal/Support " +
            "navigation helpers, and browser foreground URL assertions for the column intranet domain."
    )
    @Test
    fun givenColumnUser_whenOpeningSettingsLinks_thenColumnUrlsAreMappedOnly() = Unit

    @TestCaseId("TC-4859")
    @Category("column", "notifications")
    @Ignore(
        "Blocked: websocket-only notification flow needs column 2FA login, MLS setup, stable notification-shade " +
            "assertions, app termination helpers, and remote message delivery while app is swiped away."
    )
    @Test
    fun givenColumnWebsocketOnlyDevice_whenReceivingMessageAfterAppTermination_thenNotificationFlowIsMappedOnly() = Unit

    @TestCaseId("TC-8444")
    @Category("column", "proxy")
    @Ignore(
        "Blocked: authenticated SOCKS proxy login needs socks-access backend deep link, proxy credential UI helpers, " +
            "email 2FA, and stable create-group participant selection on that environment."
    )
    @Test
    fun givenSocksProxyBackend_whenLoggingInAndSendingGroupMessage_thenProxyFlowIsMappedOnly() = Unit
}
