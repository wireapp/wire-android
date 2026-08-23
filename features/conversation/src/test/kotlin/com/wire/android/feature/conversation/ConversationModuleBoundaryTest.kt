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

package com.wire.android.feature.conversation

import com.lemonappdev.konsist.api.Konsist
import java.io.File
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConversationModuleBoundaryTest {

    @Test
    fun conversationFeatureBuildScriptExistsAndDoesNotDependOnApp() {
        val buildScript = featureBuildScriptText()

        assertFalse(
            forbiddenAppProjectEdges.any { it.containsMatchIn(buildScript) },
            ":features:conversation must not depend on :app.",
        )
    }

    @Test
    fun appDependsOnConversationThroughTheFeatureConvention() {
        val appBuildScript = appBuildScriptText()

        assertTrue(
            inboundFeatureEdge.containsMatchIn(appBuildScript),
            ":app must declare implementationWithCoverage(projects.features.conversation).",
        )
        assertTrue(
            inboundFeatureEdge.findAll(appBuildScript).count() == 1,
            ":app must declare exactly one inbound :features:conversation edge.",
        )
    }

    private fun featureBuildScriptText(): String {
        assertTrue(featureBuildScript.isFile, "Missing :features:conversation build.gradle.kts.")
        return featureBuildScript.readText()
    }

    private fun appBuildScriptText(): String {
        assertTrue(appBuildScript.isFile, "Missing :app build.gradle.kts.")
        return appBuildScript.readText()
    }

    private companion object {
        val featureBuildScript = File(Konsist.projectRootPath, "features/conversation/build.gradle.kts")
        val appBuildScript = File(Konsist.projectRootPath, "app/build.gradle.kts")
        val forbiddenAppProjectEdges = listOf(
            Regex("""projects\s*\.\s*app\b"""),
            Regex("""project\s*\(\s*(?:path\s*=\s*)?[\"']\s*:\s*app\s*[\"']"""),
        )
        val inboundFeatureEdge = Regex(
            """implementationWithCoverage\s*\(\s*projects\s*\.\s*features\s*\.\s*conversation\s*\)""",
        )
    }
}
