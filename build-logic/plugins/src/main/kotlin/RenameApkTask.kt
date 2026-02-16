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
import com.android.build.api.artifact.ArtifactTransformationRequest
import com.android.build.api.variant.VariantOutputConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class RenameApkTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputApkFolder: DirectoryProperty

    @get:OutputDirectory
    abstract val outputApkFolder: DirectoryProperty

    @get:Input
    abstract val applicationId: Property<String>

    @get:Input
    abstract val buildType: Property<String>

    @get:Internal
    abstract val transformationRequest: Property<ArtifactTransformationRequest<RenameApkTask>>

    @TaskAction
    fun taskAction() {
        val resolvedApplicationId = applicationId.get()
        val resolvedBuildType = buildType.get()

        transformationRequest.get().submit(this) { builtArtifact ->
            val inputApkFile = File(builtArtifact.outputFile)
            val versionName = builtArtifact.versionName ?: "unknown"

            val outputApkFile = outputApkFolder
                .file(
                    buildLegacyApkFileName(
                        applicationId = resolvedApplicationId,
                        versionName = versionName,
                        buildType = resolvedBuildType,
                        isUniversalOutput = builtArtifact.outputType == VariantOutputConfiguration.OutputType.UNIVERSAL,
                    )
                )
                .get()
                .asFile

            outputApkFile.parentFile.mkdirs()
            inputApkFile.copyTo(outputApkFile, overwrite = true)
            outputApkFile
        }
    }

    data class LegacyApkFilter(
        val type: String,
        val identifier: String
    )

    companion object {
        internal fun buildLegacyApkFileName(
            applicationId: String,
            versionName: String,
            buildType: String,
            isUniversalOutput: Boolean,
        ): String {
            val safeApplicationId = sanitizeFileNameComponent(applicationId)
            val safeVersionName = sanitizeFileNameComponent(versionName)
            val safeBuildType = sanitizeFileNameComponent(buildType)

            val outputTypeSuffix = if (isUniversalOutput) {
                "-universal"
            } else {
                ""
            }

            return "$safeApplicationId-v$safeVersionName-$safeBuildType$outputTypeSuffix.apk"
        }

        private fun sanitizeFileNameComponent(value: String): String {
            val sanitized = value
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .trim('_')
            return if (sanitized.isBlank()) "unknown" else sanitized
        }
    }
}
