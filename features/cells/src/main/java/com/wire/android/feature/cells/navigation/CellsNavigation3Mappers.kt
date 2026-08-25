/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */

@file:Suppress("TooManyFunctions") // Central, mechanical boundary between route and screen models.

package com.wire.android.feature.cells.navigation

import com.wire.android.feature.cells.ui.CellFilesNavArgs
import com.wire.android.feature.cells.ui.create.file.CreateFileScreenNavArgs
import com.wire.android.feature.cells.ui.create.file.FileType
import com.wire.android.feature.cells.ui.create.folder.CreateFolderScreenNavArgs
import com.wire.android.feature.cells.ui.imageviewer.CellImageViewerNavArgs
import com.wire.android.feature.cells.ui.movetofolder.MoveToFolderNavArgs
import com.wire.android.feature.cells.ui.publiclink.PublicLinkNavArgs
import com.wire.android.feature.cells.ui.publiclink.settings.expiration.PublicLinkExpirationResult
import com.wire.android.feature.cells.ui.publiclink.settings.expiration.PublicLinkExpirationScreenNavArgs
import com.wire.android.feature.cells.ui.publiclink.settings.password.PublicLinkPasswordNavArgs
import com.wire.android.feature.cells.ui.rename.RenameNodeNavArgs
import com.wire.android.feature.cells.ui.search.DriveSearchScreenType
import com.wire.android.feature.cells.ui.search.SearchNavArgs
import com.wire.android.feature.cells.ui.tags.AddRemoveTagsNavArgs
import com.wire.android.feature.cells.ui.versioning.VersionHistoryNavArgs

internal fun CellsFilesArguments.toScreenArgs() = CellFilesNavArgs(
    conversationId = conversationId,
    screenTitle = screenTitle,
    isRecycleBin = isRecycleBin,
    breadcrumbs = breadcrumbs.toTypedArray(),
    parentFolderUuid = parentFolderUuid,
    isSearchByDefaultActive = isSearchByDefaultActive,
)

internal fun CellFilesNavArgs.toNavigation3() = CellsFilesArguments(
    conversationId = conversationId,
    screenTitle = screenTitle,
    isRecycleBin = isRecycleBin ?: false,
    breadcrumbs = breadcrumbs?.toList().orEmpty(),
    parentFolderUuid = parentFolderUuid,
    isSearchByDefaultActive = isSearchByDefaultActive ?: false,
)

internal fun CreateFolderRoute.toScreenArgs() = CreateFolderScreenNavArgs(parentUuid)
internal fun CreateFileRoute.toScreenArgs() = CreateFileScreenNavArgs(
    parentUuid,
    when (fileType) {
        CellsFileType.DOCUMENT -> FileType.DOCUMENT
        CellsFileType.PRESENTATION -> FileType.PRESENTATION
        CellsFileType.SPREADSHEET -> FileType.SPREADSHEET
    },
)
internal fun MoveToFolderRoute.toScreenArgs() =
    MoveToFolderNavArgs(currentPath, nodeToMovePath, uuid, breadcrumbs.toTypedArray())
internal fun PublicLinkRoute.toScreenArgs() =
    PublicLinkNavArgs(assetId, fileName, publicLinkId, isFolder)
internal fun PublicLinkExpirationRoute.toScreenArgs() =
    PublicLinkExpirationScreenNavArgs(linkUuid, expiresAt)
internal fun PublicLinkPasswordRoute.toScreenArgs() =
    PublicLinkPasswordNavArgs(linkUuid, passwordEnabled)
internal fun RenameNodeRoute.toScreenArgs() =
    RenameNodeNavArgs(uuid, currentPath, isFolder, nodeName)
internal fun AddRemoveTagsRoute.toScreenArgs() =
    AddRemoveTagsNavArgs(uuid, ArrayList(tags))
internal fun VersionHistoryRoute.toScreenArgs() = VersionHistoryNavArgs(uuid, fileName)
internal fun CellImageViewerRoute.toScreenArgs() =
    CellImageViewerNavArgs(localPath, contentUrl, previewUrl, contentHash, fileName)
internal fun SearchRoute.toScreenArgs() = SearchNavArgs(
    conversationId,
    when (screenType) {
        CellsSearchType.SHARED_DRIVE -> DriveSearchScreenType.SHARED_DRIVE
        CellsSearchType.DRIVE -> DriveSearchScreenType.DRIVE
    },
    parentRoute,
)

internal fun PublicLinkExpirationResult.toNavigation3() =
    PublicLinkExpirationNavigationResult(isExpirationSet, expiresAt)

internal fun PublicLinkExpirationNavigationResult.toScreenResult() =
    PublicLinkExpirationResult(isExpirationSet, expiresAt)
