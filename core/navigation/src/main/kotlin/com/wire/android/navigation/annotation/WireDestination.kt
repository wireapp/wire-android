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
package com.wire.android.navigation.annotation

import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.wire.android.navigation.wrapper.TabletDialogWrapper
import com.wire.android.navigation.wrapper.WaitUntilTransitionEndsWrapper
import kotlin.reflect.KClass

// This annotation is used for destinations that should be in the root graph
// In v2, we need to specify the graph type parameter
@Destination<RootGraph>(
    wrappers = [WaitUntilTransitionEndsWrapper::class, TabletDialogWrapper::class],
)
internal annotation class WireDestination(
    val route: String = "",  // Empty string will use the composable name
    val navArgsDelegate: KClass<*> = Nothing::class,
    val style: KClass<out DestinationStyle> = DestinationStyle.Default::class,
)
