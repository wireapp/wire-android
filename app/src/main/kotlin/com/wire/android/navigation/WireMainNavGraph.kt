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

import com.ramcosta.composedestinations.annotation.NavHostGraph

// TODO: Update this to use v2 approach with @ExternalDestination annotations
// For now, commenting out until all @WireDestination usages are migrated to @Destination
/*
object WireMainNavGraph : NavGraphSpec {
    override val route = "wire.main"
    override val startRoute = NavGraphs.wireRoot.startRoute
    val destinations: List<DestinationSpec> = NavGraphs.wireRoot.destinations
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
*/

@NavHostGraph
annotation class WireRootNavGraph(
    val start: Boolean = false
) {
    // TODO: Add @ExternalDestination annotations after migrating all @WireDestination usages
    // @ExternalDestination<DrawingCanvasScreenDestination>
    // @ExternalDestination<PublicLinkScreenDestination>
    // @ExternalDestination<ConversationFilesScreenDestination>
    // @ExternalDestination<ConversationFilesWithSlideInTransitionScreenDestination>
    // @ExternalDestination<CreateFolderScreenDestination>
    // @ExternalDestination<MoveToFolderScreenDestination>
    // @ExternalDestination<RecycleBinScreenDestination>
    // @ExternalDestination<RenameNodeScreenDestination>
    // @ExternalDestination<AddRemoveTagsScreenDestination>
    // companion object Includes
}
