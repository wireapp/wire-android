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
package com.wire.android.util.permission

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import kotlinx.coroutines.launch

/**
 * Flow that will launch the camera for taking a photo.
 * This will handle the permissions request in case there is no permission granted for the camera.
 *
 * @param onPictureTaken action that will be executed when the photo is taken - true if the photo was saved into the given [Uri]
 * @param onPermissionDenied action to be executed when the permissions is denied
 * @param onPermissionPermanentlyDenied action to be executed when the permission is permanently denied
 * @param targetPictureFileUri target file where the media will be stored
 */
@Composable
fun rememberTakePictureFlow(
    targetPictureFileUri: Uri,
    onPictureTaken: (Boolean) -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit,
): RequestLauncher {
    val snackbarHostState = LocalSnackbarHostState.current
    val message = stringResource(id = R.string.no_camera_app)
    val scope = rememberCoroutineScope()
    return rememberCheckPermissionsAndLaunchIntentRequestFlow(
        resultContract = ActivityResultContracts.TakePicture(),
        input = targetPictureFileUri,
        requiredPermissions = arrayOf(Manifest.permission.CAMERA),
        onResult = onPictureTaken,
        onAnyPermissionDenied = onPermissionDenied,
        onAnyPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
        onActivityNotFound = {
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        },
    )
}

/**
 * Flow that will launch the camera to record a video.
 * This will handle the permissions request in case there is no permission granted for the camera.
 *
 * @param onVideoCaptured action that will be executed when the video is captured - true if the video was saved into the given [Uri]
 * @param onPermissionDenied action to be executed when the permission is denied
 * @param onPermissionPermanentlyDenied action to be executed when the permission is permanently denied
 * @param targetVideoFileUri target file where the media will be stored
 */
@Composable
fun rememberCaptureVideoFlow(
    targetVideoFileUri: Uri,
    onVideoCaptured: (Boolean) -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit,
): RequestLauncher {
    val snackbarHostState = LocalSnackbarHostState.current
    val message = stringResource(id = R.string.no_camera_app)
    val scope = rememberCoroutineScope()
    return rememberCheckPermissionsAndLaunchIntentRequestFlow(
        resultContract = ActivityResultContracts.CaptureVideo(),
        input = targetVideoFileUri,
        requiredPermissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(Manifest.permission.CAMERA)
            else -> arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        },
        onResult = onVideoCaptured,
        onAnyPermissionDenied = onPermissionDenied,
        onAnyPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
        onActivityNotFound = {
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        },
    )
}

/**
 * Flow that will launch file browser to select a path where new file has to be created.
 * This will handle the permissions request in case there is no permission granted for the storage.
 *
 * @param fileName name of the file to be created
 * @param fileMimeType mime type of the file to be created
 * @param onFileCreated action that will be executed when creating a file succeeded
 * @param onPermissionDenied action to be executed when the permissions is denied
 * @param onPermissionPermanentlyDenied action to be executed when the permission is permanently denied
 */
@Composable
fun rememberCreateFileFlow(
    fileName: String,
    onFileCreated: (Uri) -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit,
    fileMimeType: String = "*/*",
): RequestLauncher {
    val snackbarHostState = LocalSnackbarHostState.current
    val message = stringResource(id = R.string.no_file_manager_app)
    val scope = rememberCoroutineScope()
    return rememberCheckPermissionsAndLaunchIntentRequestFlow(
        resultContract = ActivityResultContracts.CreateDocument(fileMimeType),
        input = fileName,
        requiredPermissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> emptyArray()
            else -> arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        },
        onResult = { onFileCreatedUri ->
            onFileCreatedUri?.let { onFileCreated(it) }
        },
        onAnyPermissionDenied = onPermissionDenied,
        onAnyPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
        onActivityNotFound = {
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        },
    )
}

/**
 * Flow that will launch file browser to select a single file.
 * This will handle the permissions request in case there is no permission granted for the storage.
 *
 * @param onFileBrowserItemPicked action that will be executed when selecting a file result from [ActivityResultContract]
 * @param onPermissionDenied action to be executed when the permissions is denied
 * @param onPermissionPermanentlyDenied action to be executed when the permission is permanently denied
 * @param fileType type of the file to be selected
 */
@Composable
fun rememberChooseSingleFileFlow(
    onFileBrowserItemPicked: (Uri?) -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit,
    fileType: FileType = FileType.Any,
): RequestLauncher {
    val snackbarHostState = LocalSnackbarHostState.current
    val message = stringResource(fileType.messageResId)
    val scope = rememberCoroutineScope()
    return rememberCheckPermissionsAndLaunchIntentRequestFlow(
        resultContract = ActivityResultContracts.GetContent(),
        input = fileType.mimeType,
        requiredPermissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> emptyArray()
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        },
        onResult = onFileBrowserItemPicked,
        onAnyPermissionDenied = onPermissionDenied,
        onAnyPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
        onActivityNotFound = {
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        },
    )
}

/**
 * Flow that will launch file browser to select multiple files.
 * This will handle the permissions request in case there is no permission granted for the storage.
 *
 * @param onFileBrowserItemPicked action that will be executed when selecting files result from [ActivityResultContract]
 * @param onPermissionDenied action to be executed when the permissions is denied
 * @param onPermissionPermanentlyDenied action to be executed when the permission is permanently denied
 * @param fileType type of the files to be selected
 */
@Composable
fun rememberChooseMultipleFilesFlow(
    onFileBrowserItemPicked: (List<Uri>) -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionPermanentlyDenied: () -> Unit,
    fileType: FileType = FileType.Any,
): RequestLauncher {
    val snackbarHostState = LocalSnackbarHostState.current
    val message = stringResource(fileType.messageResId)
    val scope = rememberCoroutineScope()
    return rememberCheckPermissionsAndLaunchIntentRequestFlow(
        resultContract = ActivityResultContracts.GetMultipleContents(),
        input = fileType.mimeType,
        requiredPermissions = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> emptyArray()
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        },
        onResult = onFileBrowserItemPicked,
        onAnyPermissionDenied = onPermissionDenied,
        onAnyPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
        onActivityNotFound = {
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        },
    )
}

enum class FileType(val mimeType: String, @StringRes val messageResId: Int) {
    Image("image/*", R.string.no_gallery_app),
    Any("*/*", R.string.no_file_manager_app)
}
