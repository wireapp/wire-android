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
package com.wire.android.navigation

import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.spec.NavGraphSpec
import com.wire.android.feature.cells.ui.destinations.AddRemoveTagsScreenDestination
import com.wire.android.feature.cells.ui.destinations.ConversationFilesScreenDestination
import com.wire.android.feature.cells.ui.destinations.ConversationFilesWithSlideInTransitionScreenDestination
import com.wire.android.feature.cells.ui.destinations.CreateFolderScreenDestination
import com.wire.android.feature.cells.ui.destinations.MoveToFolderScreenDestination
import com.wire.android.feature.cells.ui.destinations.PublicLinkScreenDestination
import com.wire.android.feature.cells.ui.destinations.RecycleBinScreenDestination
import com.wire.android.feature.cells.ui.destinations.RenameNodeScreenDestination
import com.wire.android.feature.sketch.destinations.DrawingCanvasScreenDestination
import com.wire.android.ui.NavGraphs

object WireMainNavGraph : NavGraphSpec {
    override val route = "wire.main"
    override val startRoute = NavGraphs.wireRoot.startRoute
    val destinations: List<DestinationSpec<*>> = NavGraphs.wireRoot.destinations
        .plus(DrawingCanvasScreenDestination)
        .plus(PublicLinkScreenDestination)
        .plus(ConversationFilesScreenDestination)
        .plus(ConversationFilesWithSlideInTransitionScreenDestination)
        .plus(CreateFolderScreenDestination)
        .plus(MoveToFolderScreenDestination)
        .plus(RecycleBinScreenDestination)
        .plus(RenameNodeScreenDestination)
        .plus(AddRemoveTagsScreenDestination)
    override val destinationsByRoute = destinations.associateBy { it.route }
    override val nestedNavGraphs = NavGraphs.wireRoot.nestedNavGraphs
}

// This annotation is deprecated in v2, we'll be using the generated RootGraph instead
// @NavGraph(default = true)
// annotation class WireRootNavGraph(
//     val start: Boolean = false
// )
