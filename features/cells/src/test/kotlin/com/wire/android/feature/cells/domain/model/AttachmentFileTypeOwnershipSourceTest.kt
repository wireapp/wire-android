/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.feature.cells.domain.model

import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AttachmentFileTypeOwnershipSourceTest {
    @Test
    fun givenAttachmentFileTypeContract_whenSourcesAreInspected_thenCoreOwnsClassificationAndCellsOwnsPresentation() {
        val coreContract = File(root, "core/ui-common/src/main/kotlin/com/wire/android/feature/cells/domain/model/AttachmentFileType.kt")
            .readText()
        val cellsPresentation = File(root, "features/cells/src/main/java/com/wire/android/feature/cells/domain/model/AttachmentFileTypePresentation.kt")
            .readText()

        assertTrue(coreContract.contains("enum class AttachmentFileType"))
        assertTrue(coreContract.contains("fun fromExtension"))
        assertTrue(coreContract.contains("fun fromMimeType"))
        assertFalse(coreContract.contains("com.wire.android.feature.cells.R"))
        assertFalse(coreContract.contains("fun AttachmentFileType.icon"))
        assertTrue(cellsPresentation.contains("import com.wire.android.feature.cells.R"))
        assertTrue(cellsPresentation.contains("fun AttachmentFileType.icon"))
        assertTrue(cellsPresentation.contains("fun AttachmentFileType.previewSupported"))
        assertFalse(File(root, "features/cells/src/main/java/com/wire/android/feature/cells/domain/model/AttachmentFileType.kt").exists())
    }

    private companion object {
        val root = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
    }
}
