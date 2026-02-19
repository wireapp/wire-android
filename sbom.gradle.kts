/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

// SBOM generation: produces aggregate CycloneDX JSON output.
// Plugin application and task configuration live in build.gradle.kts (requires plugin classpath).

// Set meaningful versions on internal modules for SBOM traceability.
// Without this, wire-android modules report "unspecified" in the BOM.
val wireGitHash: Provider<String> = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.map { it.trim() }

allprojects {
    version = wireGitHash.get()
}

tasks.register("generateSbom") {
    group = "reporting"
    dependsOn("cyclonedxBom")
    description = "Generate SBOM"
}
