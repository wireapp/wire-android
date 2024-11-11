/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.util

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.wire.android.appLogger

/**
 * Used to remove unnecessary metadata from the image.
 */
fun Bitmap.removeExifMetadata(exif: ExifInterface): Bitmap {
    removableExifAttributes.forEach {
        exif.setAttribute(it, null)
    }
    return this
}

/**
 * Rotates the image to its [ExifInterface.ORIENTATION_NORMAL] in case it's rotated with a different orientation than
 * landscape or portrait See more about exif interface at:
 * https://developer.android.com/reference/androidx/exifinterface/media/ExifInterface
 *
 * @param exif Exif interface for of the image to rotate
 * @return Bitmap the rotated bitmap or the same in case there is no rotation performed
 */
@Suppress("MagicNumber", "TooGenericExceptionCaught")
fun Bitmap.rotateImageToNormalOrientation(exif: ExifInterface?): Bitmap {
    val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        else -> return this
    }

    return try {
        val rotated = Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
        this.recycle()
        rotated
    } catch (exception: Exception) {
        appLogger.withTextTag("ExifHandler").w("Failed to rotate image to normal orientation", exception)
        this
    }
}

private val removableExifAttributes = arrayOf(
    // These 3, we don't remove as "might be" reused when resampling.
    // ExifInterface.TAG_ORIENTATION
    // ExifInterface.TAG_IMAGE_WIDTH,
    // ExifInterface.TAG_IMAGE_LENGTH,
    ExifInterface.TAG_BITS_PER_SAMPLE,
    ExifInterface.TAG_COMPRESSION,
    ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION,
    ExifInterface.TAG_SAMPLES_PER_PIXEL,
    ExifInterface.TAG_PLANAR_CONFIGURATION,
    ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING,
    ExifInterface.TAG_Y_CB_CR_POSITIONING,
    ExifInterface.TAG_X_RESOLUTION,
    ExifInterface.TAG_Y_RESOLUTION,
    ExifInterface.TAG_RESOLUTION_UNIT,
    ExifInterface.TAG_STRIP_OFFSETS,
    ExifInterface.TAG_ROWS_PER_STRIP,
    ExifInterface.TAG_STRIP_BYTE_COUNTS,
    ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT,
    ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
    ExifInterface.TAG_TRANSFER_FUNCTION,
    ExifInterface.TAG_WHITE_POINT,
    ExifInterface.TAG_PRIMARY_CHROMATICITIES,
    ExifInterface.TAG_Y_CB_CR_COEFFICIENTS,
    ExifInterface.TAG_REFERENCE_BLACK_WHITE,
    ExifInterface.TAG_DATETIME,
    ExifInterface.TAG_IMAGE_DESCRIPTION,
    ExifInterface.TAG_MAKE,
    ExifInterface.TAG_MODEL,
    ExifInterface.TAG_SOFTWARE,
    ExifInterface.TAG_ARTIST,
    ExifInterface.TAG_COPYRIGHT,
    ExifInterface.TAG_EXIF_VERSION,
    ExifInterface.TAG_FLASHPIX_VERSION,
    ExifInterface.TAG_COLOR_SPACE,
    ExifInterface.TAG_GAMMA,
    ExifInterface.TAG_PIXEL_X_DIMENSION,
    ExifInterface.TAG_PIXEL_Y_DIMENSION,
    ExifInterface.TAG_COMPONENTS_CONFIGURATION,
    ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL,
    ExifInterface.TAG_MAKER_NOTE,
    ExifInterface.TAG_USER_COMMENT,
    ExifInterface.TAG_RELATED_SOUND_FILE,
    ExifInterface.TAG_DATETIME_ORIGINAL,
    ExifInterface.TAG_DATETIME_DIGITIZED,
    ExifInterface.TAG_OFFSET_TIME,
    ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
    ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
    ExifInterface.TAG_SUBSEC_TIME,
    ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
    ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
    ExifInterface.TAG_EXPOSURE_TIME,
    ExifInterface.TAG_F_NUMBER,
    ExifInterface.TAG_EXPOSURE_PROGRAM,
    ExifInterface.TAG_SPECTRAL_SENSITIVITY,
    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
    ExifInterface.TAG_OECF,
    ExifInterface.TAG_SENSITIVITY_TYPE,
    ExifInterface.TAG_STANDARD_OUTPUT_SENSITIVITY,
    ExifInterface.TAG_RECOMMENDED_EXPOSURE_INDEX,
    ExifInterface.TAG_ISO_SPEED,
    ExifInterface.TAG_ISO_SPEED_LATITUDE_YYY,
    ExifInterface.TAG_ISO_SPEED_LATITUDE_ZZZ,
    ExifInterface.TAG_SHUTTER_SPEED_VALUE,
    ExifInterface.TAG_APERTURE_VALUE,
    ExifInterface.TAG_BRIGHTNESS_VALUE,
    ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
    ExifInterface.TAG_MAX_APERTURE_VALUE,
    ExifInterface.TAG_SUBJECT_DISTANCE,
    ExifInterface.TAG_METERING_MODE,
    ExifInterface.TAG_LIGHT_SOURCE,
    ExifInterface.TAG_FLASH,
    ExifInterface.TAG_SUBJECT_AREA,
    ExifInterface.TAG_FOCAL_LENGTH,
    ExifInterface.TAG_FLASH_ENERGY,
    ExifInterface.TAG_SPATIAL_FREQUENCY_RESPONSE,
    ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION,
    ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION,
    ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT,
    ExifInterface.TAG_SUBJECT_LOCATION,
    ExifInterface.TAG_EXPOSURE_INDEX,
    ExifInterface.TAG_SENSING_METHOD,
    ExifInterface.TAG_FILE_SOURCE,
    ExifInterface.TAG_SCENE_TYPE,
    ExifInterface.TAG_CFA_PATTERN,
    ExifInterface.TAG_CUSTOM_RENDERED,
    ExifInterface.TAG_EXPOSURE_MODE,
    ExifInterface.TAG_WHITE_BALANCE,
    ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
    ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
    ExifInterface.TAG_SCENE_CAPTURE_TYPE,
    ExifInterface.TAG_GAIN_CONTROL,
    ExifInterface.TAG_CONTRAST,
    ExifInterface.TAG_SATURATION,
    ExifInterface.TAG_SHARPNESS,
    ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,
    ExifInterface.TAG_SUBJECT_DISTANCE_RANGE,
    ExifInterface.TAG_IMAGE_UNIQUE_ID,
    ExifInterface.TAG_CAMERA_OWNER_NAME,
    ExifInterface.TAG_BODY_SERIAL_NUMBER,
    ExifInterface.TAG_LENS_SPECIFICATION,
    ExifInterface.TAG_LENS_MAKE,
    ExifInterface.TAG_LENS_MODEL,
    ExifInterface.TAG_LENS_SERIAL_NUMBER,
    ExifInterface.TAG_GPS_VERSION_ID,
    ExifInterface.TAG_GPS_LATITUDE_REF,
    ExifInterface.TAG_GPS_LATITUDE,
    ExifInterface.TAG_GPS_LONGITUDE_REF,
    ExifInterface.TAG_GPS_LONGITUDE,
    ExifInterface.TAG_GPS_ALTITUDE_REF,
    ExifInterface.TAG_GPS_ALTITUDE,
    ExifInterface.TAG_GPS_TIMESTAMP,
    ExifInterface.TAG_GPS_SATELLITES,
    ExifInterface.TAG_GPS_STATUS,
    ExifInterface.TAG_GPS_MEASURE_MODE,
    ExifInterface.TAG_GPS_DOP,
    ExifInterface.TAG_GPS_SPEED_REF,
    ExifInterface.TAG_GPS_SPEED,
    ExifInterface.TAG_GPS_TRACK_REF,
    ExifInterface.TAG_GPS_TRACK,
    ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
    ExifInterface.TAG_GPS_IMG_DIRECTION,
    ExifInterface.TAG_GPS_MAP_DATUM,
    ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
    ExifInterface.TAG_GPS_DEST_LATITUDE,
    ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
    ExifInterface.TAG_GPS_DEST_LONGITUDE,
    ExifInterface.TAG_GPS_DEST_BEARING_REF,
    ExifInterface.TAG_GPS_DEST_BEARING,
    ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
    ExifInterface.TAG_GPS_DEST_DISTANCE,
    ExifInterface.TAG_GPS_PROCESSING_METHOD,
    ExifInterface.TAG_GPS_AREA_INFORMATION,
    ExifInterface.TAG_GPS_DATESTAMP,
    ExifInterface.TAG_GPS_DIFFERENTIAL,
    ExifInterface.TAG_GPS_H_POSITIONING_ERROR,
    ExifInterface.TAG_INTEROPERABILITY_INDEX,
    ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH,
    ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH,
    ExifInterface.TAG_DNG_VERSION,
    ExifInterface.TAG_DEFAULT_CROP_SIZE,
    ExifInterface.TAG_ORF_THUMBNAIL_IMAGE,
    ExifInterface.TAG_ORF_PREVIEW_IMAGE_START,
    ExifInterface.TAG_ORF_PREVIEW_IMAGE_LENGTH,
    ExifInterface.TAG_ORF_ASPECT_FRAME,
    ExifInterface.TAG_RW2_SENSOR_BOTTOM_BORDER,
    ExifInterface.TAG_RW2_SENSOR_LEFT_BORDER,
    ExifInterface.TAG_RW2_SENSOR_RIGHT_BORDER,
    ExifInterface.TAG_RW2_SENSOR_TOP_BORDER,
    ExifInterface.TAG_RW2_ISO,
    ExifInterface.TAG_RW2_JPG_FROM_RAW,
    ExifInterface.TAG_XMP,
    ExifInterface.TAG_NEW_SUBFILE_TYPE,
    ExifInterface.TAG_SUBFILE_TYPE,
)
