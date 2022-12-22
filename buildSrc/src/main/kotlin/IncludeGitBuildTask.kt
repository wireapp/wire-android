import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream

/**
 * This task will try to get the hash from the environment git log entries and write it to an asset file.
 * This file will be located at [VERSION_FILE] for the app usage.
 */
open class IncludeGitBuildTask : DefaultTask() {

    private val VERSION_FILE = "app/src/main/assets/version.txt"

    init {
        group = "release"
        description = "Get the current GIT hash and write it to the application, if possible."
    }

    @TaskAction
    fun processGitBuildIdentifier() {
        println("\uD83D\uDD27 Running git hash parser to build.")
        extractAndWriteHashToFile()
    }

    private fun extractAndWriteHashToFile() {
        runCatching {
            val hash = "git rev-parse --short HEAD".runCommand().orEmpty()
            println("\uD83D\uDD27 Git hash: $hash")
            writeToFile(hash)
        }.onFailure {
            println("\u26A0\uFE0F Failed to get git hash: ${it.message}")
            writeToFile()
        }
    }

    /**
     * Write the given [hash] to the [VERSION_FILE].
     */
    private fun writeToFile(hash: String = "0000000") {
        FileOutputStream(File(project.rootDir, VERSION_FILE)).use {
            it.write("$hash".toByteArray())
        }
        println("\u2705 Successfully wrote git hash [$hash] to file.")
    }
}
