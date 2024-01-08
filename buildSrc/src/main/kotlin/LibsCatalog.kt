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

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.provider.Provider

val Project.versionCatalog: VersionCatalog
    get() {
        val catalogs = extensions.getByType(VersionCatalogsExtension::class.java)
        return catalogs.named("libs")
    }

fun Project.findLibrary(name: String): Provider<MinimalExternalModuleDependency> {
    return versionCatalog.findLibrary(name).get()
}

fun Project.findVersion(name: String): VersionConstraint {
    return versionCatalog.findVersion(name).get()
}
