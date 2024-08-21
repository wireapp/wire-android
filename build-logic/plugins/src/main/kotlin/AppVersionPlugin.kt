import com.wire.android.gradle.version.Versionizer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
class AppVersionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            project.tasks.register("generateVersionFile", Task::class.java) {
                // clean the repo from a probably existing version.txt
                if (file("$projectDir/version.txt").exists()) {
                    println("deleting existing version.txt file for safety reasons")
                    file("$projectDir/version.txt").delete()
                }

                val currentTime = LocalDateTime.now()
                val versnisor = Versionizer(projectDir, currentTime)
                val versionCode = versnisor.versionCode
                val versionName = "${AndroidApp.versionName}-${AndroidApp.leastSignificantVersionCode}"
                val buildTime = currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) ?: error("Failed to get build time")
                val appName = "com.wire"
                // git commit hash code
                val gitRevision = "git rev-parse --short HEAD".execute().text.trim()
                println("VersionCode: $versionCode")
                println("VersionName: $versionName")
                println("Revision: $gitRevision")
                println("Buildtime: $buildTime")
                println("Application-name: $appName")

                // output the data to a file in app/version.txt
                file("$projectDir/version.txt").writeText(
                    """
                    |VersionCode: $versionCode
                    |VersionName: $versionName
                    |Revision: $gitRevision
                    |Buildtime: $buildTime
                    |Application-name: $appName
                    """.trimMargin()
                )
            }
        }
    }
}
