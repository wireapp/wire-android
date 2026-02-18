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
package com.wire.android.navigation.annotation.app

import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.Destination.Companion.COMPOSABLE_NAME
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.wire.android.navigation.WireRootNavGraph
import com.wire.android.navigation.wrapper.TabletDialogWrapper
import com.wire.android.navigation.wrapper.WaitUntilTransitionEndsWrapper
import com.wire.android.ui.authentication.create.common.CreateAccountNavGraph
import com.wire.android.ui.authentication.create.common.CreatePersonalAccountNavGraph
import com.wire.android.ui.authentication.create.common.CreateTeamAccountNavGraph
import com.wire.android.ui.authentication.login.LoginNavGraph
import com.wire.android.ui.authentication.login.NewLoginNavGraph
import com.wire.android.ui.home.HomeNavGraph
import com.wire.android.ui.home.newconversation.common.NewConversationNavGraph
import com.wire.android.ui.userprofile.teammigration.PersonalToTeamMigrationNavGraph
import kotlin.reflect.KClass

@Destination<WireRootNavGraph>(
    wrappers = [WaitUntilTransitionEndsWrapper::class, TabletDialogWrapper::class],
)
internal annotation class WireRootDestination(
    val route: String = COMPOSABLE_NAME,
    val start: Boolean = false,
    val navArgs: KClass<*> = Nothing::class,
    val style: KClass<out DestinationStyle> = DestinationStyle.Default::class,
)

@Destination<HomeNavGraph>(
    wrappers = [WaitUntilTransitionEndsWrapper::class, TabletDialogWrapper::class],
)
internal annotation class WireHomeDestination(
    val route: String = COMPOSABLE_NAME,
    val start: Boolean = false,
    val navArgs: KClass<*> = Nothing::class,
    val style: KClass<out DestinationStyle> = DestinationStyle.Default::class,
)

@Destination<LoginNavGraph>(
    wrappers = [WaitUntilTransitionEndsWrapper::class, TabletDialogWrapper::class],
)
internal annotation class WireLoginDestination(
    val route: String = COMPOSABLE_NAME,
    val start: Boolean = false,
    val navArgs: KClass<*> = Nothing::class,
    val style: KClass<out DestinationStyle> = DestinationStyle.Default::class,
)

@Destination<NewLoginNavGraph>(
    wrappers = [WaitUntilTransitionEndsWrapper::class, TabletDialogWrapper::class],
)
internal annotation class WireNewLoginDestination(
    val route: String = COMPOSABLE_NAME,
    val start: Boolean = false,
    val navArgs: KClass<*> = Nothing::class,
    val style: KClass<out DestinationStyle> = DestinationStyle.Default::class,
)

@Destination<CreateAccountNavGraph>(
    wrappers = [WaitUntilTransitionEndsWrapper::class, TabletDialogWrapper::class],
)
internal annotation class WireCreateAccountDestination(
    val route: String = COMPOSABLE_NAME,
    val start: Boolean = false,
    val navArgs: KClass<*> = Nothing::class,
    val style: KClass<out DestinationStyle> = DestinationStyle.Default::class,
)

@Destination<CreatePersonalAccountNavGraph>(
    wrappers = [WaitUntilTransitionEndsWrapper::class, TabletDialogWrapper::class],
)
internal annotation class WireCreatePersonalAccountDestination(
    val route: String = COMPOSABLE_NAME,
    val start: Boolean = false,
    val navArgs: KClass<*> = Nothing::class,
    val style: KClass<out DestinationStyle> = DestinationStyle.Default::class,
)

@Destination<CreateTeamAccountNavGraph>(
    wrappers = [WaitUntilTransitionEndsWrapper::class, TabletDialogWrapper::class],
)
internal annotation class WireCreateTeamAccountDestination(
    val route: String = COMPOSABLE_NAME,
    val start: Boolean = false,
    val navArgs: KClass<*> = Nothing::class,
    val style: KClass<out DestinationStyle> = DestinationStyle.Default::class,
)

@Destination<NewConversationNavGraph>(
    wrappers = [WaitUntilTransitionEndsWrapper::class, TabletDialogWrapper::class],
)
internal annotation class WireNewConversationDestination(
    val route: String = COMPOSABLE_NAME,
    val start: Boolean = false,
    val navArgs: KClass<*> = Nothing::class,
    val style: KClass<out DestinationStyle> = DestinationStyle.Default::class,
)

@Destination<PersonalToTeamMigrationNavGraph>(
    wrappers = [WaitUntilTransitionEndsWrapper::class, TabletDialogWrapper::class],
)
internal annotation class WirePersonalToTeamMigrationDestination(
    val route: String = COMPOSABLE_NAME,
    val start: Boolean = false,
    val navArgs: KClass<*> = Nothing::class,
    val style: KClass<out DestinationStyle> = DestinationStyle.Default::class,
)
