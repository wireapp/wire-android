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
package com.wire.android.navigation

import androidx.compose.runtime.Composable

// Todo(docs): Add ADR about this change introduced in navigation styles for tablets, which requires
//  adjusting navigation styles for certain destinations when on tablets.
// See ADR-0010:
// docs/adr/0010-tablet-dialog-navigation-parity-after-compose-destinations-upgrade.md
@Composable
fun AdjustDestinationStylesForTablets() = Unit
