/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

@file:Suppress("LongMethod", "CyclomaticComplexMethod", "TooManyFunctions")

package com.wire.android.feature.cells.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.wire.android.audioplayer.AudioPlayer
import com.wire.android.feature.cells.ui.CellsFilesNavigation
import com.wire.android.feature.cells.ui.ConversationFilesRouteScreen
import com.wire.android.feature.cells.ui.ConversationFilesSlideRouteScreen
import com.wire.android.feature.cells.ui.addRemoveTagsViewModel
import com.wire.android.feature.cells.ui.cellViewModel
import com.wire.android.feature.cells.ui.create.file.CreateFileRouteScreen
import com.wire.android.feature.cells.ui.create.file.FileType
import com.wire.android.feature.cells.ui.create.folder.CreateFolderRouteScreen
import com.wire.android.feature.cells.ui.createFileViewModel
import com.wire.android.feature.cells.ui.createFolderViewModel
import com.wire.android.feature.cells.ui.imageviewer.CellImageViewerScreenContent
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.moveToFolderViewModel
import com.wire.android.feature.cells.ui.movetofolder.MoveToFolderRouteScreen
import com.wire.android.feature.cells.ui.publicLinkExpirationViewModel
import com.wire.android.feature.cells.ui.publicLinkPasswordViewModel
import com.wire.android.feature.cells.ui.publicLinkViewModel
import com.wire.android.feature.cells.ui.publiclink.PublicLinkRouteScreen
import com.wire.android.feature.cells.ui.publiclink.PublicLinkScreenData
import com.wire.android.feature.cells.ui.publiclink.settings.expiration.PublicLinkExpirationRouteScreen
import com.wire.android.feature.cells.ui.publiclink.settings.password.PublicLinkPasswordRouteScreen
import com.wire.android.feature.cells.ui.recyclebin.RecycleBinRouteScreen
import com.wire.android.feature.cells.ui.rename.RenameNodeRouteScreen
import com.wire.android.feature.cells.ui.renameNodeViewModel
import com.wire.android.feature.cells.ui.search.SearchRouteScreen
import com.wire.android.feature.cells.ui.search.sort.SortCriteriaNavArg
import com.wire.android.feature.cells.ui.searchCellViewModel
import com.wire.android.feature.cells.ui.searchScreenViewModel
import com.wire.android.feature.cells.ui.tags.AddRemoveTagsRouteScreen
import com.wire.android.feature.cells.ui.versionHistoryViewModel
import com.wire.android.feature.cells.ui.versioning.VersionHistoryRouteScreen
import com.wire.android.pdfviewer.PdfViewer
import com.wire.android.navigation.navigation3.WireNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.videoplayer.VideoPlayer
import com.wire.navigation.WireBackStackMode
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultRequestId
import com.wire.navigation.WireNavigationCommand
import com.wire.navigation.WireSessionId

/** Feature-owned Navigation 3 renderer used by the app catalog. */
@Composable
internal fun CellsNavigation3RouteScreen(
    route: CellsRoute,
    runtime: WireNavigation3Runtime,
    onExitCells: () -> Unit,
) {
    var booleanRequestIdValue by rememberSaveable(route.entryId.value, "boolean-result") {
        mutableStateOf<String?>(null)
    }
    var expirationRequestIdValue by rememberSaveable(route.entryId.value, "expiration-result") {
        mutableStateOf<String?>(null)
    }
    val navigateBack: () -> Unit = {
        if (!runtime.navigator.goBack()) onExitCells()
    }
    val filesNavigation = remember(route.entryId, runtime, onExitCells) {
        Navigation3CellsFilesNavigation(runtime, route.sessionId, onExitCells)
    }

    when (route) {
        is ConversationFilesRoute -> AnimatedVisibility(visible = true) {
            ConversationFilesRouteScreen(
                navigation = filesNavigation,
                animatedVisibilityScope = this,
                viewModel = cellViewModel(route.args.toScreenArgs()),
            )
        }

        is ConversationFilesSlideRoute -> AnimatedVisibility(visible = true) {
            ConversationFilesSlideRouteScreen(
                navigation = filesNavigation,
                cellFilesNavArgs = route.args.toScreenArgs(),
                animatedVisibilityScope = this,
                viewModel = cellViewModel(route.args.toScreenArgs()),
            )
        }

        is RecycleBinRoute -> RecycleBinRouteScreen(
            navigation = filesNavigation,
            cellViewModel = cellViewModel(route.args.toScreenArgs()),
        )

        is CreateFolderRoute -> CreateFolderRouteScreen(
            onNavigateBack = navigateBack,
            onCreated = {
                completeBooleanResult(runtime, true)
            },
            createFolderViewModel = createFolderViewModel(route.toScreenArgs()),
        )

        is CreateFileRoute -> CreateFileRouteScreen(
            onNavigateBack = navigateBack,
            onCreated = {
                completeBooleanResult(runtime, true)
            },
            createFileViewModel = createFileViewModel(route.toScreenArgs()),
        )

        is MoveToFolderRoute -> {
            val viewModel = moveToFolderViewModel(route.toScreenArgs())
            ConsumeNavigation3Result(
                runtime = runtime,
                requestIdValue = booleanRequestIdValue,
                resultType = CellsBooleanNavigation3ResultType,
                onConsumed = { booleanRequestIdValue = null },
                onResult = { if (it.value) viewModel.loadFolders() },
            )
            MoveToFolderRouteScreen(
                onNavigateBack = navigateBack,
                onCloseFlow = {
                    popConsecutive(runtime, MoveToFolderRoute.ROUTE_ID, onExitCells)
                },
                onNavigateBackSteps = { steps ->
                    repeat(steps) {
                        if (runtime.navigator.currentRoute?.routeId != MoveToFolderRoute.ROUTE_ID) return@repeat
                        runtime.navigator.goBack()
                    }
                },
                onOpenCreateFolder = { uuid ->
                    runtime.navigateForResult(
                        CreateFolderRoute(route.sessionId, uuid),
                        CellsBooleanNavigation3ResultType,
                    ).also { booleanRequestIdValue = it?.value }
                },
                onOpenFolder = { path, nodePath, nodeUuid, breadcrumbs ->
                    runtime.navigator.navigate(
                        WireNavigationCommand(
                            MoveToFolderRoute(
                                route.sessionId,
                                path,
                                nodePath,
                                nodeUuid,
                                breadcrumbs,
                            ),
                            launchSingleTop = false,
                        )
                    )
                },
                moveToFolderViewModel = viewModel,
            )
        }

        is PublicLinkRoute -> {
            val viewModel = publicLinkViewModel(route.toScreenArgs())
            ConsumeNavigation3Result(
                runtime = runtime,
                requestIdValue = booleanRequestIdValue,
                resultType = CellsBooleanNavigation3ResultType,
                onConsumed = { booleanRequestIdValue = null },
                onResult = { viewModel.onPasswordUpdate(it.value) },
            )
            ConsumeNavigation3Result(
                runtime = runtime,
                requestIdValue = expirationRequestIdValue,
                resultType = PublicLinkExpirationNavigation3ResultType,
                onConsumed = { expirationRequestIdValue = null },
                onResult = { viewModel.onExpirationUpdate(it.toScreenResult()) },
            )
            PublicLinkRouteScreen(
                onNavigateBack = navigateBack,
                onOpenPasswordSettings = { linkUuid, passwordEnabled ->
                    runtime.navigateForResult(
                        PublicLinkPasswordRoute(route.sessionId, linkUuid, passwordEnabled),
                        CellsBooleanNavigation3ResultType,
                    ).also { booleanRequestIdValue = it?.value }
                },
                onOpenExpirationSettings = { linkUuid, expiresAt ->
                    runtime.navigateForResult(
                        PublicLinkExpirationRoute(route.sessionId, linkUuid, expiresAt),
                        PublicLinkExpirationNavigation3ResultType,
                    ).also { expirationRequestIdValue = it?.value }
                },
                viewModel = viewModel,
            )
        }

        is PublicLinkExpirationRoute -> PublicLinkExpirationRouteScreen(
            onResult = { result ->
                if (!runtime.completeCurrentAndPop(
                        PublicLinkExpirationNavigation3ResultType,
                        WireNavResult.Value(result.toNavigation3()),
                    )
                ) {
                    runtime.navigator.goBack()
                }
            },
            viewModel = publicLinkExpirationViewModel(route.toScreenArgs()),
        )

        is PublicLinkPasswordRoute -> PublicLinkPasswordRouteScreen(
            onResult = { completeBooleanResult(runtime, it) },
            viewModel = publicLinkPasswordViewModel(route.toScreenArgs()),
        )

        is RenameNodeRoute -> RenameNodeRouteScreen(
            onNavigateBack = navigateBack,
            renameNodeViewModel = renameNodeViewModel(route.toScreenArgs()),
        )

        is AddRemoveTagsRoute -> AddRemoveTagsRouteScreen(
            onNavigateBack = navigateBack,
            addRemoveTagsViewModel = addRemoveTagsViewModel(route.toScreenArgs()),
        )

        is VersionHistoryRoute -> VersionHistoryRouteScreen(
            onNavigateBack = navigateBack,
            versionHistoryViewModel = versionHistoryViewModel(route.toScreenArgs()),
        )

        is CellImageViewerRoute -> CellImageViewerScreenContent(
            localPath = route.localPath,
            contentUrl = route.contentUrl,
            previewUrl = route.previewUrl,
            contentHash = route.contentHash,
            fileName = route.fileName,
            onNavigateBack = navigateBack,
        )

        is VideoPlayerRoute -> VideoPlayer(
            localPath = route.localPath,
            contentUrl = route.contentUrl,
            fileName = route.fileName,
            onNavigateBack = navigateBack,
        )

        is AudioPlayerRoute -> AudioPlayer(
            localPath = route.localPath,
            contentUrl = route.contentUrl,
            fileName = route.fileName,
            onNavigateBack = navigateBack,
        )
        is PdfViewerRoute -> PdfViewer(
            localPath = route.localPath,
            assetId = route.assetId,
            remotePath = route.remotePath,
            conversationId = route.conversationId,
            assetSize = route.assetSize,
            fileName = route.fileName,
            onNavigateBack = navigateBack,
        )
        is SearchRoute -> AnimatedVisibility(visible = true) {
            SearchRouteScreen(
                navigation = filesNavigation,
                animatedVisibilityScope = this,
                cellViewModel = searchCellViewModel(route.toScreenArgs()),
                searchScreenViewModel = searchScreenViewModel(route.toScreenArgs()),
            )
        }
    }
}

private class Navigation3CellsFilesNavigation(
    private val runtime: WireNavigation3Runtime,
    private val sessionId: WireSessionId,
    private val onExit: () -> Unit,
) : CellsFilesNavigation {
    override fun back() {
        if (!runtime.navigator.goBack()) onExit()
    }

    override fun createFolder(parentUuid: String?) {
        runtime.navigator.navigate(WireNavigationCommand(CreateFolderRoute(sessionId, parentUuid)))
    }

    override fun createFile(parentUuid: String, fileType: FileType) {
        runtime.navigator.navigate(
            WireNavigationCommand(
                CreateFileRoute(
                    sessionId,
                    parentUuid,
                    when (fileType) {
                        FileType.DOCUMENT -> CellsFileType.DOCUMENT
                        FileType.PRESENTATION -> CellsFileType.PRESENTATION
                        FileType.SPREADSHEET -> CellsFileType.SPREADSHEET
                    },
                )
            )
        )
    }

    override fun recycleBin(args: com.wire.android.feature.cells.ui.CellFilesNavArgs, popConsecutive: Boolean) {
        runtime.navigator.navigate(
            WireNavigationCommand(
                RecycleBinRoute(sessionId, args.toNavigation3()),
                backStackMode = if (popConsecutive) {
                    WireBackStackMode.POP_CONSECUTIVE_SAME_ROUTES
                } else {
                    WireBackStackMode.NONE
                },
            )
        )
    }

    override fun search(conversationId: String, sortCriteria: SortCriteriaNavArg?) {
        runtime.navigator.navigate(WireNavigationCommand(SearchRoute(sessionId, conversationId, sortCriteria)))
    }

    override fun folder(args: com.wire.android.feature.cells.ui.CellFilesNavArgs) {
        runtime.navigator.navigate(
            WireNavigationCommand(
                ConversationFilesSlideRoute(sessionId, args.toNavigation3()),
                launchSingleTop = false,
            )
        )
    }

    override fun publicLink(data: PublicLinkScreenData) {
        runtime.navigator.navigate(
            WireNavigationCommand(
                PublicLinkRoute(sessionId, data.assetId, data.fileName, data.linkId, data.isFolder)
            )
        )
    }

    override fun move(currentPath: String, nodePath: String, uuid: String) {
        runtime.navigator.navigate(
            WireNavigationCommand(MoveToFolderRoute(sessionId, currentPath, nodePath, uuid))
        )
    }

    override fun rename(node: CellNodeUi) {
        runtime.navigator.navigate(
            WireNavigationCommand(
                RenameNodeRoute(
                    sessionId,
                    node.uuid,
                    node.remotePath,
                    node is CellNodeUi.Folder,
                    node.name,
                )
            )
        )
    }

    override fun tags(node: CellNodeUi) {
        runtime.navigator.navigate(WireNavigationCommand(AddRemoveTagsRoute(sessionId, node.uuid, node.tags)))
    }

    override fun versionHistory(uuid: String, fileName: String) {
        runtime.navigator.navigate(WireNavigationCommand(VersionHistoryRoute(sessionId, uuid, fileName)))
    }

    override fun image(file: CellNodeUi.File) {
        runtime.navigator.navigate(
            WireNavigationCommand(
                CellImageViewerRoute(
                    sessionId,
                    file.localPath,
                    file.contentUrl,
                    file.previewUrl,
                    file.contentHash,
                    file.name,
                )
            )
        )
    }

    override fun video(file: CellNodeUi.File) {
        runtime.navigator.navigate(
            WireNavigationCommand(
                VideoPlayerRoute(sessionId, file.localPath, file.contentUrl, file.name)
            )
        )
    }

    override fun audio(file: CellNodeUi.File) {
        runtime.navigator.navigate(
            WireNavigationCommand(
                AudioPlayerRoute(sessionId, file.localPath, file.contentUrl, file.name)
            )
        )
    }

    override fun pdf(file: CellNodeUi.File) {
        runtime.navigator.navigate(
            WireNavigationCommand(
                PdfViewerRoute(
                    sessionId = sessionId,
                    localPath = file.localPath,
                    assetId = file.uuid,
                    remotePath = file.remotePath,
                    conversationId = file.conversationId,
                    assetSize = file.size ?: 0L,
                    fileName = file.name,
                )
            )
        )
    }
}

private fun completeBooleanResult(runtime: WireNavigation3Runtime, value: Boolean) {
    if (!runtime.completeCurrentAndPop(
            CellsBooleanNavigation3ResultType,
            WireNavResult.Value(CellsBooleanResult(value)),
        )
    ) {
        runtime.navigator.goBack()
    }
}

@Composable
private fun <T> ConsumeNavigation3Result(
    runtime: WireNavigation3Runtime,
    requestIdValue: String?,
    resultType: WireNavigation3ResultType<T>,
    onConsumed: () -> Unit,
    onResult: (T) -> Unit,
) {
    LaunchedEffect(requestIdValue, runtime.navigator.currentRoute?.entryId) {
        val id = requestIdValue?.let(::WireNavResultRequestId) ?: return@LaunchedEffect
        when (val result = runtime.consumeResult(id, resultType)) {
            is WireNavResult.Value -> onResult(result.value)
            WireNavResult.Canceled -> Unit
            null -> return@LaunchedEffect
        }
        onConsumed()
    }
}

private fun popConsecutive(
    runtime: WireNavigation3Runtime,
    routeId: String,
    onExit: () -> Unit,
) {
    while (runtime.navigator.currentRoute?.routeId == routeId) {
        if (!runtime.navigator.goBack()) {
            onExit()
            return
        }
    }
}
