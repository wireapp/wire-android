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
package com.wire.android.di.metro

import dev.zacsweers.metro.Scope

/**
 * Scope of a single logged-in account, implemented by the `AppSessionViewModelGraph` extension.
 *
 * Lives in `:core:di` rather than `:app` so that feature modules can scope bindings that depend on
 * account-scoped kalium use cases — anything reachable from `CellsScope` and friends must not be
 * `AppScope`, or it would outlive the account it belongs to.
 */
@Scope
annotation class MetroSessionScope
