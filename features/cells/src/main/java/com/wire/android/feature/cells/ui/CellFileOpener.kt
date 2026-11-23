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
package com.wire.android.feature.cells.ui

import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.ui.edit.OnlineEditor
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.pdfRenditionUrl
import com.wire.android.feature.cells.util.FileHelper
import com.wire.kalium.cells.domain.usecase.GetEditorUrlUseCase
import com.wire.kalium.cells.domain.usecase.GetWireCellConfigurationUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.featureConfig.CollaboraEdition
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath

/**
 * Opens files: in an in-app viewer when there is one for them, in the online editor for documents, otherwise in
 * another app. Also shares them. A user with viewer access only is never handed a file over to another app.
 */
class CellFileOpener @Inject constructor(
    private val fileHelper: FileHelper,
    private val getEditorUrl: GetEditorUrlUseCase,
    private val onlineEditor: OnlineEditor,
    private val openFileDownloadController: OpenFileDownloadController,
    private val getWireCellsConfig: GetWireCellConfigurationUseCase,
    private val featureFlags: CellFeatureFlags,
) {

    /** Whether the online editor is available, known once [loadConfig] completed. */
    var isCollaboraEnabled: Boolean = false
        private set

    suspend fun loadConfig() {
        isCollaboraEnabled = getWireCellsConfig()?.collabora != CollaboraEdition.NO
    }

    /**
     * Both the editor and the PDF rendition are served by the backend, so while offline such a document falls back to
     * the regular open path — a local copy, if there is one.
     */
    internal fun isEditableDocument(file: CellNodeUi.File, isOnline: Boolean): Boolean =
        file.isEditSupported && isCollaboraEnabled && featureFlags.collaboraIntegration && isOnline

    /**
     * Documents, presentations and spreadsheets open in the online editor. A user with viewer access can't edit them, so
     * they are shown read-only through the PDF rendition the backend generates for them. Without such a rendition (still
     * processing, failed or unsupported) there is nothing a viewer may be shown, which is reported the same way as for
     * any other file a viewer can't open.
     */
    internal fun openEditableDocument(file: CellNodeUi.File, scope: CoroutineScope, actions: CellActionSink) {
        when {
            !(file.isViewerOnly && featureFlags.drivePermissionsEnabled) -> edit(file.uuid, scope, actions)
            file.pdfRenditionUrl() != null -> actions.sendAction(OpenPdfViewer(file))
            else -> actions.sendAction(ShowError(CellError.OTHER_ERROR))
        }
    }

    internal fun edit(nodeUuid: String, scope: CoroutineScope, actions: CellActionSink) = scope.launch {
        getEditorUrl(nodeUuid)
            .onSuccess { url ->
                if (url != null) onlineEditor.open(url) else actions.sendAction(ShowEditErrorDialog(nodeUuid))
            }
            .onFailure { actions.sendAction(ShowEditErrorDialog(nodeUuid)) }
    }

    /** Opens [file] from its local copy. */
    internal fun openLocalFile(file: CellNodeUi.File, isViewerOnly: Boolean, actions: CellActionSink) =
        openInAppOrElse(file, isViewerOnly, actions) {
            file.localPath?.let { path ->
                fileHelper.openAssetFileWithExternalApp(
                    localPath = path.toPath(),
                    assetName = file.name,
                    mimeType = file.mimeType,
                    onError = { actions.sendAction(ShowError(CellError.NO_APP_FOUND)) },
                )
            }
        }

    /** Opens [file] straight from its URL, without downloading it first. */
    internal fun openContentUrl(file: CellNodeUi.File, isViewerOnly: Boolean, actions: CellActionSink) =
        openInAppOrElse(file, isViewerOnly, actions) {
            file.contentUrl?.let { url ->
                fileHelper.openAssetUrlWithExternalApp(
                    url = url,
                    mimeType = file.mimeType,
                    onError = { actions.sendAction(ShowError(CellError.NO_APP_FOUND)) },
                )
            }
        }

    /** Downloads [file] to open it, see [OpenFileDownloadController]. */
    internal fun startDownload(
        file: CellNodeUi.File,
        scope: CoroutineScope,
        actions: CellActionSink,
        onOpenFile: (CellNodeUi.File) -> Unit,
    ) {
        openFileDownloadController.start(
            scope = scope,
            cellNode = file,
            onOpenFile = onOpenFile,
            onError = { actions.sendAction(ShowError(it)) },
        )
    }

    fun cancelDownload(uuid: String, scope: CoroutineScope) {
        openFileDownloadController.cancel(uuid, scope)
    }

    internal fun share(file: CellNodeUi.File, actions: CellActionSink) {
        val localPath = file.localPath ?: run {
            actions.sendAction(ShowError(CellError.OTHER_ERROR))
            return
        }
        fileHelper.shareFileChooser(
            assetDataPath = localPath.toPath(),
            assetName = file.name,
            mimeType = file.mimeType,
            onError = { actions.sendAction(ShowError(CellError.OTHER_ERROR)) },
        )
    }

    private fun openInAppOrElse(
        file: CellNodeUi.File,
        isViewerOnly: Boolean,
        actions: CellActionSink,
        openWithExternalApp: () -> Unit,
    ) {
        val inAppViewer = inAppViewerAction(file)
        when {
            inAppViewer != null -> actions.sendAction(inAppViewer)
            isViewerOnly -> actions.sendAction(ShowError(CellError.FILE_NOT_SUPPORTED))
            else -> openWithExternalApp()
        }
    }

    /** The in-app viewer that can show [file], or null when the file has to be handed over to another app. */
    private fun inAppViewerAction(file: CellNodeUi.File): CellViewAction? = when (file.assetType) {
        AttachmentFileType.IMAGE -> OpenImageViewer(file).takeIf { featureFlags.inAppImageViewerEnabled }
        AttachmentFileType.VIDEO -> OpenVideoViewer(file)
        AttachmentFileType.AUDIO -> OpenAudioPlayer(file)
        AttachmentFileType.PDF -> OpenPdfViewer(file).takeIf { file.localPath != null || file.remotePath != null }
        else -> null
    }
}
