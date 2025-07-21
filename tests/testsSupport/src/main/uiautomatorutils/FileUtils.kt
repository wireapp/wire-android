import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

fun deleteDownloadedFilesByBaseName(baseName: String) {
    try {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val listCommand = "ls /sdcard/Download"
        val fileListOutput = device.executeShellCommand(listCommand)

        val matchingFiles = fileListOutput
            .split("\n")
            .map { it.trim() }
            .filter { it.startsWith(baseName) }

        if (matchingFiles.isEmpty()) {
            println("⚠️ No files found with base name '$baseName'")
            return
        }

        for (file in matchingFiles) {
            val deleteCommand = "rm -f /sdcard/Download/$file"
            val result = device.executeShellCommand(deleteCommand)
            println("✅ Deleted: $file. Output: $result")
        }

    } catch (e: Exception) {
        println("❌ Error while deleting files: ${e.message}")
    }
}
