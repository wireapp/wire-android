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
package customization

import com.android.build.api.dsl.ApplicationExtension
import flavor.ProductFlavors
import java.io.File

fun ApplicationExtension.overrideResourcesForAllFlavors(
    customResourcesRootDir: File,
    projectDir: File
) {

    sourceSets {
        ProductFlavors.all.forEach {
            getByName(it.buildName).apply {
                val resDir = File(projectDir, "src/${it.buildName}/res")
                resDir.mkdirs()
                println("Copying files from '${customResourcesRootDir.absolutePath}' into '${resDir.absolutePath}'")

                customResourcesRootDir.walkTopDown().filter { !it.isDirectory }.forEach { customContent ->
                    val relativePath = customContent.relativeTo(customResourcesRootDir).path
                    val targetFile = File(resDir, relativePath)
                    customContent.copyTo(targetFile, overwrite = true)
                }
            }
        }
    }
}
