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

package com.wire.android.navigation.navigation3

import androidx.compose.ui.window.DialogProperties
import com.wire.navigation.WireRoute
import kotlin.reflect.KClass

/**
 * The dialog configuration used by Wire's typed tablet presentation policy.
 */
val WireTabletDialogProperties: DialogProperties
    get() = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        usePlatformDefaultWidth = true,
        decorFitsSystemWindows = false,
    )

/**
 * Selects responsive entry presentation using typed [WireRoute] instances.
 *
 * The policy deliberately contains no Android [android.content.res.Configuration] access. The
 * composable host supplies the current layout mode, while Navigation 3 metadata callbacks only
 * evaluate the stable route predicate captured here.
 */
class WireResponsivePresentationPolicy private constructor(
    private val tabletDialogRoutePredicate: (WireRoute) -> Boolean,
    internal val tabletDialogProperties: DialogProperties,
) {

    /**
     * Resolves a route's presentation without changing explicit presentation on phones.
     */
    fun resolve(
        route: WireRoute,
        isTablet: Boolean,
        defaultPresentation: WireEntryPresentation = WireEntryPresentation.Default,
    ): WireEntryPresentation =
        if (shouldPresentAsTabletDialog(route, isTablet)) {
            WireEntryPresentation.dialog(tabletDialogProperties)
        } else {
            defaultPresentation
        }

    internal fun shouldPresentAsTabletDialog(route: WireRoute, isTablet: Boolean): Boolean =
        isTablet && tabletDialogRoutePredicate(route)

    companion object {
        /** A no-op policy for hosts that do not use responsive dialog presentation. */
        val None: WireResponsivePresentationPolicy = WireResponsivePresentationPolicy(
            tabletDialogRoutePredicate = { false },
            tabletDialogProperties = WireTabletDialogProperties,
        )

        /**
         * Creates a policy backed by concrete route types instead of generated base-route strings.
         */
        fun tabletDialogsFor(
            routeTypes: Set<KClass<out WireRoute>>,
            properties: DialogProperties = WireTabletDialogProperties,
        ): WireResponsivePresentationPolicy = WireResponsivePresentationPolicy(
            tabletDialogRoutePredicate = { route -> route::class in routeTypes },
            tabletDialogProperties = properties,
        )

        /**
         * Creates a policy for typed route families whose decision also depends on route data.
         */
        fun tabletDialogsWhere(
            properties: DialogProperties = WireTabletDialogProperties,
            predicate: (WireRoute) -> Boolean,
        ): WireResponsivePresentationPolicy = WireResponsivePresentationPolicy(
            tabletDialogRoutePredicate = predicate,
            tabletDialogProperties = properties,
        )
    }
}
