/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */

package com.wire.android.feature.cells.ui

import com.wire.android.feature.cells.ui.create.file.FileType
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.publiclink.PublicLinkScreenData
import com.wire.android.feature.cells.ui.search.sort.SortCriteriaNavArg

@Suppress("TooManyFunctions")
internal interface CellsFilesNavigation {
    fun back()
    fun createFolder(parentUuid: String?)
    fun createFile(parentUuid: String, fileType: FileType)
    fun recycleBin(args: CellFilesNavArgs, popConsecutive: Boolean = false)
    fun search(conversationId: String, sortCriteria: SortCriteriaNavArg? = null)
    fun folder(args: CellFilesNavArgs)
    fun publicLink(data: PublicLinkScreenData)
    fun move(currentPath: String, nodePath: String, uuid: String)
    fun rename(node: CellNodeUi)
    fun tags(node: CellNodeUi)
    fun versionHistory(uuid: String, fileName: String)
    fun image(file: CellNodeUi.File)
    fun video(file: CellNodeUi.File)
    fun audio(file: CellNodeUi.File)
    fun pdf(file: CellNodeUi.File)
}

@Suppress("TooManyFunctions")
internal object NoOpCellsFilesNavigation : CellsFilesNavigation {
    override fun back() = Unit
    override fun createFolder(parentUuid: String?) = Unit
    override fun createFile(parentUuid: String, fileType: FileType) = Unit
    override fun recycleBin(args: CellFilesNavArgs, popConsecutive: Boolean) = Unit
    override fun search(conversationId: String, sortCriteria: SortCriteriaNavArg?) = Unit
    override fun folder(args: CellFilesNavArgs) = Unit
    override fun publicLink(data: PublicLinkScreenData) = Unit
    override fun move(currentPath: String, nodePath: String, uuid: String) = Unit
    override fun rename(node: CellNodeUi) = Unit
    override fun tags(node: CellNodeUi) = Unit
    override fun versionHistory(uuid: String, fileName: String) = Unit
    override fun image(file: CellNodeUi.File) = Unit
    override fun video(file: CellNodeUi.File) = Unit
    override fun audio(file: CellNodeUi.File) = Unit
    override fun pdf(file: CellNodeUi.File) = Unit
}
