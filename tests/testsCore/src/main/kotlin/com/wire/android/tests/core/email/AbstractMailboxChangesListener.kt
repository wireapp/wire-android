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
package com.wire.android.tests.core.email

import com.wire.android.tests.core.config.BackendConnectionsReader
import com.wire.android.tests.core.utils.Timedelta
import com.wire.android.tests.core.utils.ZetaLogger
import java.util.concurrent.Callable
import java.util.logging.Logger

abstract class AbstractMailboxChangesListener(
    parentMBox: Any,
    expectedHeaders: Map<String, String>,
    protected val timeout: Timedelta,
    protected val rejectMessagesBefore: Timedelta
) : Callable<String> {

    protected val expectedHeaders: Map<String, String> = HashMap(expectedHeaders)
    protected val parentMBox: Any = parentMBox

    protected val log: Logger = ZetaLogger.getLog(AbstractMailboxChangesListener::class.simpleName)

    protected fun getParentMbox(): Any = this.parentMBox
}
