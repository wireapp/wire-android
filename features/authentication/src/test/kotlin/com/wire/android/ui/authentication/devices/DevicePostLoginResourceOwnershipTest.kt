package com.wire.android.ui.authentication.devices

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DevicePostLoginResourceOwnershipTest {
    @Test
    fun `device gate presentation owns exact slide transition and terminal action adaptation`() {
        val root = repositoryRoot()
        val featureRoot = root.resolve("features/authentication/src/main/kotlin/com/wire/android/ui/authentication/devices")
        val transition = Files.readString(featureRoot.resolve("DeviceAuthenticationTransitions.kt"))
        val register = Files.readString(featureRoot.resolve("register/RegisterDeviceContent.kt"))
        val remove = Files.readString(featureRoot.resolve("remove/RemoveDeviceContent.kt"))

        assertFalse(listOf(transition, register, remove).any { it.contains("com.wire.android.navigation") })
        assertTrue(transition.contains("Spring.StiffnessMediumLow"))
        assertTrue(transition.contains("fullWidth / 3"))
        assertTrue(transition.contains("fadeIn(animationSpec = deviceFadeAnimationSpec)"))
        assertTrue(transition.contains("togetherWith(fadeOut(animationSpec = deviceFadeAnimationSpec))"))
        assertTrue(register.contains("transitionSpec = { deviceAuthenticationSlideTransition() }"))
        assertTrue(remove.contains("transitionSpec = { deviceAuthenticationSlideTransition() }"))
        assertTrue(remove.contains("is OnComplete -> onComplete(action)"))
        assertFalse(remove.contains("HandleActions(viewModel.actions, onComplete)"))
    }

    @Test
    fun `register and loading error resources move exactly with every qualifier`() {
        val root = repositoryRoot()
        val app = root.resolve("app/src/main/res")
        val feature = root.resolve("features/authentication/src/main/res")
        resourceNames.forEach { name ->
            assertTrue(definitions(app, name).isEmpty(), "app still owns $name")
        }
        expectedQualifiers.forEach { (name, qualifiers) ->
            assertEquals(qualifiers, definitions(feature, name).keys)
        }
    }

    private fun definitions(root: Path, name: String): Map<String, String> = Files.walk(root).use { paths ->
        paths.filter { Files.isRegularFile(it) && it.fileName.toString() == "strings.xml" }
            .map { path ->
                Regex("""<string name=\"$name\"[^>]*>.*?</string>""")
                    .find(Files.readString(path))
                    ?.value
                    ?.let { path.parent.fileName.toString() to it }
            }.filter { it != null }.map { it!! }.toList().toMap()
    }

    private fun repositoryRoot(): Path =
        generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) { it.parent }
            .first { Files.isDirectory(it.resolve("app/src/main/kotlin")) }

    private companion object {
        val resourceNames = setOf(
            "register_device_title", "register_device_text", "label_add_device", "devices_loading_error",
        )
        val expectedQualifiers = mapOf(
            "register_device_title" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu",
                "values-it", "values-ja", "values-pl", "values-pt", "values-ru", "values-si",
            ),
            "register_device_text" to setOf(
                "values", "values-de", "values-es", "values-fr", "values-hr", "values-hu",
                "values-it", "values-ja", "values-pl", "values-pt", "values-ru", "values-si", "values-tr",
            ),
            "label_add_device" to setOf(
                "values", "values-ar", "values-cs", "values-de", "values-es", "values-et",
                "values-fr", "values-hr", "values-hu", "values-it", "values-ja", "values-lt",
                "values-pl", "values-pt", "values-ru", "values-si", "values-sv", "values-tr", "values-uk",
            ),
            "devices_loading_error" to setOf("values", "values-hu", "values-pt", "values-ru", "values-si"),
        )
    }
}
