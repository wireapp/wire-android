/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */

package com.wire.android.feature.cells.navigation

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavResultContract
import com.wire.navigation.WireNavResultContractId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

@Serializable
sealed interface CellsRoute : SessionRoute

@Serializable
data class CellsFilesArguments(
    val conversationId: String? = null,
    val screenTitle: String? = null,
    val isRecycleBin: Boolean = false,
    val breadcrumbs: List<String> = emptyList(),
    val parentFolderUuid: String? = null,
    val isSearchByDefaultActive: Boolean = false,
)

@Serializable
data class ConversationFilesRoute(
    override val sessionId: WireSessionId,
    val args: CellsFilesArguments,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : CellsRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "cells/conversation_files_screen" }
}

@Serializable
data class ConversationFilesSlideRoute(
    override val sessionId: WireSessionId,
    val args: CellsFilesArguments,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : CellsRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "cells/conversation_files_with_slide_in_transition_screen" }
}

@Serializable
data class RecycleBinRoute(
    override val sessionId: WireSessionId,
    val args: CellsFilesArguments,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : CellsRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "cells/recycle_bin_screen" }
}

@Serializable
data class CreateFolderRoute(
    override val sessionId: WireSessionId,
    val parentUuid: String?,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : CellsRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "cells/create_folder_screen" }
}

@Serializable
enum class CellsFileType { DOCUMENT, PRESENTATION, SPREADSHEET }

@Serializable
data class CreateFileRoute(
    override val sessionId: WireSessionId,
    val parentUuid: String,
    val fileType: CellsFileType,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : CellsRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "cells/create_file_screen" }
}

@Serializable
data class MoveToFolderRoute(
    override val sessionId: WireSessionId,
    val currentPath: String,
    val nodeToMovePath: String,
    val uuid: String,
    val breadcrumbs: List<String> = emptyList(),
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : CellsRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "cells/move_to_folder_screen" }
}

@Serializable
data class PublicLinkRoute(
    override val sessionId: WireSessionId,
    val assetId: String,
    val fileName: String,
    val publicLinkId: String?,
    val isFolder: Boolean,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : CellsRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "cells/public_link_screen" }
}

@Serializable
data class PublicLinkExpirationRoute(
    override val sessionId: WireSessionId,
    val linkUuid: String,
    val expiresAt: Long?,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : CellsRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "cells/public_link_expiration_screen" }
}

@Serializable
data class PublicLinkPasswordRoute(
    override val sessionId: WireSessionId,
    val linkUuid: String,
    val passwordEnabled: Boolean,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : CellsRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "cells/public_link_password_screen" }
}

@Serializable
data class RenameNodeRoute(
    override val sessionId: WireSessionId,
    val uuid: String?,
    val currentPath: String?,
    val isFolder: Boolean?,
    val nodeName: String?,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : CellsRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "cells/rename_node_screen" }
}

@Serializable
data class AddRemoveTagsRoute(
    override val sessionId: WireSessionId,
    val uuid: String,
    val tags: List<String>,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : CellsRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "cells/add_remove_tags_screen" }
}

@Serializable
data class VersionHistoryRoute(
    override val sessionId: WireSessionId,
    val uuid: String,
    val fileName: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : CellsRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "cells/version_history_screen" }
}

@Serializable
data class CellImageViewerRoute(
    override val sessionId: WireSessionId,
    val localPath: String? = null,
    val contentUrl: String? = null,
    val previewUrl: String? = null,
    val contentHash: String? = null,
    val fileName: String? = null,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : CellsRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "cells/cell_image_viewer_screen" }
}

@Serializable
data class VideoPlayerRoute(
    override val sessionId: WireSessionId,
    val localPath: String? = null,
    val contentUrl: String? = null,
    val fileName: String? = null,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : CellsRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "cells/video_player_screen" }
}

@Serializable
data class AudioPlayerRoute(
    override val sessionId: WireSessionId,
    val localPath: String? = null,
    val contentUrl: String? = null,
    val fileName: String? = null,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : CellsRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "cells/audio_player_screen" }
}

@Serializable
enum class CellsSearchType { SHARED_DRIVE, DRIVE }

@Serializable
data class SearchRoute(
    override val sessionId: WireSessionId,
    val conversationId: String? = null,
    val screenType: CellsSearchType = CellsSearchType.SHARED_DRIVE,
    val parentRoute: String? = null,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : CellsRoute {
    override val routeId = ROUTE_ID
    companion object { const val ROUTE_ID = "cells/search_screen" }
}

@Serializable
data class CellsBooleanResult(val value: Boolean)

val CellsBooleanResultContract = WireNavResultContract<CellsBooleanResult>(
    WireNavResultContractId("cells.boolean")
)

@Serializable
data class PublicLinkExpirationNavigationResult(
    val isExpirationSet: Boolean,
    val expiresAt: Long?,
)

val PublicLinkExpirationResultContract =
    WireNavResultContract<PublicLinkExpirationNavigationResult>(
        WireNavResultContractId("cells.public-link-expiration")
    )
