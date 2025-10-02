import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.IOException

private const val DOWNLOAD_DIR = "/sdcard/Download"

fun deleteDownloadedFilesContaining(keyword: String, dir: String = DOWNLOAD_DIR) {
    try {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val listCommand = "ls $dir"
        val fileListOutput = device.executeShellCommand(listCommand)

        val matchingFiles = fileListOutput
            .split("\n")
            .map { it.trim() }
            .filter { it.contains(keyword, ignoreCase = true) }

        if (matchingFiles.isEmpty()) {
            println("No files found containing '$keyword'")
            return
        }

        for (file in matchingFiles) {
            if (file.isBlank()) continue
            val deleteCommand = "rm -f $dir/$file"
            val result = device.executeShellCommand(deleteCommand)
            println("Deleted: $file. Output: $result")
        }
    } catch (e: IOException) {
        println("Error while deleting files: ${e.message}")
    }
}
