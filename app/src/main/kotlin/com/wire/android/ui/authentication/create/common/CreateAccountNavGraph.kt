/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.authentication.create.common

import com.ramcosta.composedestinations.annotation.NavGraph
import com.wire.android.navigation.WireRootNavGraph

@Deprecated("These destinations belongs to the old registration flow, please use the new one [CreateAccountNavGraph]")
@WireRootNavGraph
@NavGraph
annotation class CreatePersonalAccountNavGraph(
    val start: Boolean = false
)

@Deprecated("These destinations belongs to the old registration flow, please use the new one [CreateAccountNavGraph]")
@WireRootNavGraph
@NavGraph
annotation class CreateTeamAccountNavGraph(
    val start: Boolean = false
)

@WireRootNavGraph
@NavGraph
annotation class CreateAccountNavGraph(
    val start: Boolean = false
)
