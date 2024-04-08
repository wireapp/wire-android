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

package scripts

import IncludeGitBuildTask

plugins {
    id("com.android.application") apply false
}

// TODO: Extract to a convention plugin
project.tasks.register("includeGitBuildIdentifier", IncludeGitBuildTask::class) {
    println("> Registering Task :includeGitBuildIdentifier")
}

project.afterEvaluate {
<<<<<<< HEAD
    project.tasks.matching { it.name.startsWith("bundle") || it.name.startsWith("assemble") }.configureEach {
        dependsOn("includeGitBuildIdentifier")
=======
    project.tasks.matching {
        it.name.startsWith("merge") &&
                it.name.endsWith("Assets") ||
                it.name.startsWith("lintVitalAnalyze")
>>>>>>> 37ce2d8d6 (fix: lintVitalAnalyze failing because of dependenciesVersionTask (#2858))
    }
        .configureEach {
            dependsOn(gitIdTask)
            dependsOn(dependenciesVersionTask)
        }
}
