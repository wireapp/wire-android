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
package com.wire.android.navigation.annotation.features.cells

import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.Destination.Companion.COMPOSABLE_NAME
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.wire.android.feature.cells.navigation.CellsNavGraph
import com.wire.android.navigation.wrapper.TabletDialogWrapper
import com.wire.android.navigation.wrapper.WaitUntilTransitionEndsWrapper
import kotlin.reflect.KClass

@Destination<CellsNavGraph>(
    wrappers = [WaitUntilTransitionEndsWrapper::class, TabletDialogWrapper::class],
)
internal annotation class WireCellsDestination(
    val route: String = COMPOSABLE_NAME,
    val start: Boolean = false,
    val navArgs: KClass<*> = Nothing::class,
    val style: KClass<out DestinationStyle> = DestinationStyle.Default::class,
)
