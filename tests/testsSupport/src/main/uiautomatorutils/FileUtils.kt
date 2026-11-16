import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import uiautomatorutils.UiWaitUtils
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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

fun createOneKbFileInDeviceDownloadsFolder(fileName: String) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.executeShellCommand("dd if=/dev/zero of=$DOWNLOAD_DIR/$fileName bs=1024 count=1")
}

fun getDownloadedFileNames(dir: String = DOWNLOAD_DIR): Set<String> {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    return device.executeShellCommand("ls $dir")
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
}

fun waitForNewDownloadedFileName(
    fileNamePrefix: String,
    existingFiles: Set<String>,
    timeout: Duration = 15.seconds,
    dir: String = DOWNLOAD_DIR
): String {
    var downloadedFile: String? = null
    val downloaded = UiWaitUtils.retryUntilTimeout(timeout) {
        downloadedFile = (getDownloadedFileNames(dir) - existingFiles)
            .firstOrNull { it.startsWith(fileNamePrefix) }
        downloadedFile != null
    }

    if (!downloaded) {
        throw AssertionError("A new '$fileNamePrefix' file was not downloaded within ${timeout.inWholeSeconds} seconds.")
    }
    return requireNotNull(downloadedFile)
}

/** Creates a valid, identifiable PDF that Android's file picker can discover in Downloads. */
fun createPdfInDeviceDownloadsFolder(fileName: String, text: String): Uri =
    createMultipagePdfInDeviceDownloadsFolder(fileName, listOf(text))

// Writes each PDF once and publishes it to Downloads only after all pages have been written.
@Suppress("ThrowsCount", "TooGenericExceptionCaught")
private fun writePdfToDeviceDownloads(fileName: String, drawPages: (PdfDocument) -> Unit): Uri {
    val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Could not create PDF '$fileName' in Downloads")
    } else {
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        check(directory.exists() || directory.mkdirs()) { "Could not create Downloads directory" }
        Uri.fromFile(File(directory, fileName))
    }
    val document = PdfDocument()
    return try {
        drawPages(document)
        val output = resolver.openOutputStream(uri) ?: throw IOException("Could not open PDF output '$fileName'")
        output.use(document::writeTo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        }
        uri
    } catch (exception: Exception) {
        deleteDeviceDownload(uri)
        throw exception
    } finally {
        document.close()
    }
}

fun deleteDeviceDownload(uri: Uri) {
    if (uri.scheme == "file") {
        val path = requireNotNull(uri.path)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // QR fixtures return file URIs even though their Downloads entries were created through MediaStore.
            InstrumentationRegistry.getInstrumentation().targetContext.contentResolver.delete(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                "${MediaStore.MediaColumns.DATA} = ?",
                arrayOf(path)
            )
        }
        File(path).delete()
    } else {
        InstrumentationRegistry.getInstrumentation().targetContext.contentResolver.delete(uri, null, null)
    }
}

/** Creates numbered, identifiable PDF pages using the shared Downloads writer. */
@Suppress("MagicNumber")
fun createMultipagePdfInDeviceDownloadsFolder(fileName: String, pageTexts: List<String>): Uri {
    require(pageTexts.isNotEmpty()) { "A PDF fixture must contain at least one page" }
    return writePdfToDeviceDownloads(fileName) { document ->
        pageTexts.forEachIndexed { index, text ->
            val page = document.startPage(PdfDocument.PageInfo.Builder(300, 400, index + 1).create())
            page.canvas.drawText(text, 20f, 40f, Paint().apply { textSize = 14f })
            document.finishPage(page)
        }
    }
}

/** Adds distinct image data to each page so cancellation tests use a real, multi-megabyte PDF. */
@Suppress("MagicNumber")
fun createLargePdfInDeviceDownloadsFolder(fileName: String, pageTexts: List<String>): Uri {
    require(pageTexts.isNotEmpty()) { "A PDF fixture must contain at least one page" }
    val random = kotlin.random.Random(0)
    return writePdfToDeviceDownloads(fileName) { document ->
        pageTexts.forEachIndexed { index, text ->
            val image = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
            try {
                val pixels =
                    IntArray(image.width * image.height) { Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256)) }
                image.setPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
                val page = document.startPage(PdfDocument.PageInfo.Builder(300, 400, index + 1).create())
                page.canvas.drawText(text, 20f, 40f, Paint().apply { textSize = 14f })
                page.canvas.drawBitmap(image, null, android.graphics.Rect(20, 70, 280, 370), null)
                document.finishPage(page)
            } finally {
                image.recycle()
            }
        }
    }
}
