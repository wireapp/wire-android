import android.graphics.Bitmap
import android.os.Environment
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
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

@Suppress("MagicNumber")
    object QrCodeTestUtils {
        fun createQrImageInDeviceDownloadsFolder(text: String): File {
            val size = 500

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

            val downloads = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            if (!downloads.exists()) downloads.mkdirs()

            val file = File(downloads, "$text.png")

            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }

            return file
        }
    }
