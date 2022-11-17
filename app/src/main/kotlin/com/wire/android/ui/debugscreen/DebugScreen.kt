package com.wire.android.ui.debugscreen

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.WireSwitch
import com.wire.android.ui.common.button.WireButton
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.home.settings.SettingsItem
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.getDeviceId
import com.wire.android.util.getMimeType
import com.wire.android.util.getUrisOfFilesInDirectory
import com.wire.android.util.multipleFileSharingIntent
import java.io.File

@Composable
fun DebugScreen() {
    val debugScreenViewModel: DebugScreenViewModel = hiltViewModel()
    val debugContentState = rememberDebugContentState(debugScreenViewModel.logPath)

    DebugContent(
        debugScreenState = debugScreenViewModel.state,
        debugContentState = debugContentState,
        onLoggingEnabledChange = debugScreenViewModel::setLoggingEnabledState,
        onDeleteLogs = debugScreenViewModel::deleteLogs,
        navigateBack = debugScreenViewModel::navigateBack,
        onForceLatestDevelopmentApiChange = debugScreenViewModel::forceUpdateApiVersions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugContent(
    debugScreenState: DebugScreenState,
    debugContentState: DebugContentState,
    onLoggingEnabledChange: (Boolean) -> Unit,
    onDeleteLogs: () -> Unit,
    navigateBack: () -> Unit,
    onForceLatestDevelopmentApiChange: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.label_debug_title),
                elevation = 0.dp,
                navigationIconType = NavigationIconType.Back,
                onNavigationPressed = navigateBack
            )
        }
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(internalPadding)
        ) {
            MlsOptions(
                keyPackagesCount = debugScreenState.keyPackagesCount,
                mlsClientId = debugScreenState.mslClientId,
                mlsErrorMessage = debugScreenState.mlsErrorMessage
            )

            LogOptions(
                deviceId = debugContentState.deviceId,
                isLoggingEnabled = debugScreenState.isLoggingEnabled,
                onLoggingEnabledChange = onLoggingEnabledChange,
                onDeleteLogs = onDeleteLogs,
                onShareLogs = debugContentState::shareLogs,
                onCopyDeviceId = debugContentState::copyToClipboard
            )

            ClientIdOptions(
                debugScreenState.currentClientId,
                debugContentState::copyToClipboard
            )

            if (BuildConfig.DEBUG) {
                DevelopmentApiVersioningOptions(onForceLatestDevelopmentApiChange = onForceLatestDevelopmentApiChange)
            }
        }
    }
}

@Composable
private fun MlsOptions(
    keyPackagesCount: Int,
    mlsClientId: String,
    mlsErrorMessage: String
) {
    if (mlsErrorMessage.isNotEmpty()) {
        SettingsItem(
            title = mlsErrorMessage
        )
    } else {
        Column {
            FolderHeader(
                name = stringResource(R.string.label_mls_option_title)
            )

            SettingsItem(
                title = stringResource(R.string.label_key_packages_count, keyPackagesCount)
            )

            SettingsItem(
                title = stringResource(R.string.label_mls_client_id, mlsClientId)
            )
        }
    }
}

@Composable
private fun LogOptions(
    deviceId: String?,
    isLoggingEnabled: Boolean,
    onLoggingEnabledChange: (Boolean) -> Unit,
    onDeleteLogs: () -> Unit,
    onShareLogs: () -> Unit,
    onCopyDeviceId: (String) -> Unit
) {
    Column {
        FolderHeader(stringResource(R.string.label_logs_option_title))

        EnableLoggingSwitch(
            isEnabled = isLoggingEnabled,
            onCheckedChange = onLoggingEnabledChange
        )

        SettingsItem(
            title = stringResource(R.string.label_share_logs),
            trailingIcon = R.drawable.ic_entypo_share,
            onIconPressed = Clickable(
                enabled = true,
                onClick = onShareLogs
            )
        )

        SettingsItem(
            title = stringResource(R.string.label_delete_logs),
            trailingIcon = R.drawable.ic_delete,
            onIconPressed = Clickable(
                enabled = true,
                onClick = onDeleteLogs
            )
        )

        if (deviceId != null) {
            SettingsItem(
                title = stringResource(R.string.label_device_id, deviceId),
                trailingIcon = R.drawable.ic_copy,
                onIconPressed = Clickable(
                    enabled = true,
                    onClick = { onCopyDeviceId(deviceId) }
                )
            )
        }
    }
}

@Composable
private fun ClientIdOptions(
    currentClientId: String,
    onCopyClientId: (String) -> Unit,
) {
    Column {
        FolderHeader(stringResource(R.string.label_client_option_title))
        SettingsItem(
            title = currentClientId,
            trailingIcon = R.drawable.ic_copy,
            onIconPressed = Clickable(
                enabled = true,
                onClick = { onCopyClientId(currentClientId) }
            )
        )
    }
}

@Composable
private fun EnableLoggingSwitch(
    isEnabled: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    RowItemTemplate(
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = stringResource(R.string.label_enable_logging),
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WireSwitch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(end = dimensions().spacing16x)
            )
        }
    )
}

@Composable
fun DevelopmentApiVersioningOptions(
    onForceLatestDevelopmentApiChange: () -> Unit
) {
    FolderHeader(stringResource(R.string.debug_settings_api_versioning_title))
    RowItemTemplate(modifier = Modifier.wrapContentWidth(),
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = stringResource(R.string.debug_settings_force_api_versioning_update),
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WirePrimaryButton(
                onClick = onForceLatestDevelopmentApiChange,
                text = stringResource(R.string.debug_settings_force_api_versioning_update_button_text),
                fillMaxWidth = false
            )
        }
    )
}

@Composable
fun rememberDebugContentState(logPath: String): DebugContentState {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    return remember {
        DebugContentState(
            context,
            clipboardManager,
            logPath
        )
    }
}

data class DebugContentState(
    val context: Context,
    val clipboardManager: ClipboardManager,
    val logPath: String
) {

    val deviceId: String?
        get() = context.getDeviceId()

    fun copyToClipboard(text: String) {
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(
            context,
            context.getText(R.string.label_text_copied),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun shareLogs() {
        val dir = File(logPath).parentFile
        val fileUris = context.getUrisOfFilesInDirectory(dir)
        val intent = context.multipleFileSharingIntent(fileUris)
        // The first log file is simply text, not compressed. Get its mime type separately
        // and set it as the mime type for the intent.
        intent.type = fileUris.firstOrNull()?.getMimeType(context) ?: "text/plain"
        // Get all other mime types and add them
        val mimeTypes = fileUris.drop(1).mapNotNull { it.getMimeType(context) }
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
        context.startActivity(intent)
    }
}



