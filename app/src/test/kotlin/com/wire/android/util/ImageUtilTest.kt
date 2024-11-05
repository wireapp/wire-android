package com.wire.android.util

import android.app.Application
import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ImageUtilTest {

    @Test
    fun `given an image with exif metadata, when resampling and removal marked, then output should not contain metadata`() {
        val originalImage = File(javaClass.getResource("/rich-exif-sample.jpg")!!.path)

        // when
        val resampledImage = ImageUtil.resample(originalImage.readBytes(), ImageUtil.ImageSizeClass.Medium, shouldRemoveMetadata = true)
        val exif = ExifInterface(resampledImage.inputStream())

        // then
        assertTrue(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) == null)
        assertTrue(exif.getAttribute(ExifInterface.TAG_DATETIME) == null)
    }

    @Test
    fun `given an image with exif metadata, when resampling and removal not marked, then output should contain metadata`() {
        // given
        val originalImage = File(javaClass.getResource("/rich-exif-sample.jpg")!!.path)

        // when
        val resampledImage = ImageUtil.resample(originalImage.readBytes(), ImageUtil.ImageSizeClass.Medium, shouldRemoveMetadata = false)
        val exif = ExifInterface(resampledImage.inputStream())

        // then
        assertTrue(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null)
        assertTrue(exif.getAttribute(ExifInterface.TAG_DATETIME) != null)
    }
}
