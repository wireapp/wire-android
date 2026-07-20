import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import kotlin.time.Duration

private const val DOWNLOAD_DIR = "/sdcard/Download"

data class DeviceTestFile(
    val sourceFile: File,
    val size: Long,
    val sha256: String,
) {
    val fileName: String = sourceFile.name
}

data class DeviceFileFingerprint(
    val path: String,
    val size: Long,
    val sha256: String,
)

@Suppress("MagicNumber")
fun createDeterministicFile(directory: File, fileName: String, size: Long): DeviceTestFile {
    require(size >= 0) { "File size must be non-negative." }
    require(directory.exists() || directory.mkdirs()) { "Could not create directory '${directory.absolutePath}'." }
    val sourceFile = File(directory, fileName)

    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(64 * 1024) { index -> ((index * 31 + 17) % 251).toByte() }

    FileOutputStream(sourceFile).use { output ->
        var remaining = size
        while (remaining > 0) {
            val count = minOf(remaining, buffer.size.toLong()).toInt()
            output.write(buffer, 0, count)
            digest.update(buffer, 0, count)
            remaining -= count
        }
    }

    return DeviceTestFile(sourceFile, size, digest.digest().toHexString())
}

fun waitUntilAppExternalFileMatches(
    appPackage: String,
    expected: DeviceTestFile,
    timeout: Duration,
): DeviceFileFingerprint {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val deadline = SystemClock.uptimeMillis() + timeout.inWholeMilliseconds
    var lastObserved: String? = null

    while (SystemClock.uptimeMillis() < deadline) {
        val paths = findAppExternalFiles(device, appPackage, expected.fileName)
        for (path in paths) {
            val size = device.executeShellCommand("stat -c %s ${path.shellQuoted()}").trim().toLongOrNull()
                ?: continue
            lastObserved = "$path ($size bytes)"
            if (size == expected.size) {
                val sha256 = device.executeShellCommand("sha256sum ${path.shellQuoted()}")
                    .trim()
                    .substringBefore(' ')
                if (sha256 == expected.sha256) {
                    return DeviceFileFingerprint(path, size, sha256)
                }
                lastObserved = "$path ($size bytes, sha256=$sha256)"
            }
        }
        SystemClock.sleep(500)
    }

    throw AssertionError(
        "No downloaded file '${expected.fileName}' matched size=${expected.size} and " +
                "sha256=${expected.sha256} within ${timeout.inWholeMilliseconds}ms. Last observed: $lastObserved"
    )
}

fun waitUntilAppExternalFileIsAbsent(
    appPackage: String,
    fileName: String,
    timeout: Duration,
) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val deadline = SystemClock.uptimeMillis() + timeout.inWholeMilliseconds
    while (SystemClock.uptimeMillis() < deadline) {
        if (findAppExternalFiles(device, appPackage, fileName).isEmpty()) return
        SystemClock.sleep(250)
    }
    throw AssertionError("Partial download '$fileName' was not removed within ${timeout.inWholeMilliseconds}ms.")
}

fun waitUntilAppExternalFileIsPartial(
    appPackage: String,
    expected: DeviceTestFile,
    timeout: Duration,
) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val deadline = SystemClock.uptimeMillis() + timeout.inWholeMilliseconds

    while (SystemClock.uptimeMillis() < deadline) {
        findAppExternalFiles(device, appPackage, expected.fileName).forEach { path ->
            val size = device.executeShellCommand("stat -c %s ${path.shellQuoted()}").trim().toLongOrNull()
                ?: return@forEach
            if (size in 1 until expected.size) {
                return
            }
        }
        SystemClock.sleep(100)
    }

    throw AssertionError(
        "Download '${expected.fileName}' did not produce a partial file within ${timeout.inWholeMilliseconds}ms."
    )
}

fun deleteAppExternalFilesContaining(appPackage: String, keyword: String) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val root = "/sdcard/Android/data/$appPackage/files"
    device.executeShellCommand("find ${root.shellQuoted()} -type f")
        .lineSequence()
        .map(String::trim)
        .filter { path -> path.startsWith(root) && path.substringAfterLast('/').contains(keyword, ignoreCase = true) }
        .forEach { path -> device.executeShellCommand("rm -f ${path.shellQuoted()}") }
}

private fun findAppExternalFiles(device: UiDevice, appPackage: String, fileName: String): List<String> {
    val root = "/sdcard/Android/data/$appPackage/files"
    return device.executeShellCommand(
        "find ${root.shellQuoted()} -type f -name ${fileName.shellQuoted()}"
    ).lineSequence().map(String::trim).filter { it.startsWith(root) }.toList()
}

private fun ByteArray.toHexString(): String = joinToString(separator = "") { byte ->
    (byte.toInt() and 0xff).toString(16).padStart(2, '0')
}

private fun String.shellQuoted(): String = "'${replace("'", "'\\''")}'"

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

@Suppress("MagicNumber", "ThrowsCount", "TooGenericExceptionCaught")
object QrCodeTestUtils {
    /**
     * Generates a QR PNG and stores it in the device Downloads folder so test flows can pick it from DocumentsUI.
     */
    fun createQrImageInDeviceDownloadsFolder(text: String): File {
        val size = 500
        val fileName = "$text.png"
        val bitMatrix = QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size
        )
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloads.exists()) downloads.mkdirs()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android Q+ requires writing shared files through MediaStore (scoped storage).
            val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                // Keep the item hidden until writing is complete.
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Failed to create MediaStore entry for $fileName")

            try {
                resolver.openOutputStream(uri)?.use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                } ?: throw IOException("Failed to open output stream for $uri")

                // Publish file to Downloads once fully written.
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (e: Exception) {
                // Avoid leaving broken entries in MediaStore on partial write failures.
                resolver.delete(uri, null, null)
                throw e
            }

            return File(downloads, fileName)
        }

        val file = File(downloads, fileName)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return file
    }
}
