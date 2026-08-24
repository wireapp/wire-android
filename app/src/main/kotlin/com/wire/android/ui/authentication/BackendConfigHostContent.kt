package com.wire.android.ui.authentication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.wire.android.R
import com.wire.android.ui.common.R as CommonR
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import kotlinx.coroutines.launch

@Composable
fun MissingBackendConfigContent(
    modifier: Modifier = Modifier,
    showTitle: Boolean = false,
    centerText: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    errorText: String? = null,
    isLoading: Boolean = false,
    onConfigurationLinkEntered: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    BackendConfigFormContent(
        text = backendConfigText(),
        onConfigurationLinkEntered = onConfigurationLinkEntered,
        onDefaultConfigurationLinkEntered = context::openBackendConfig,
        trailingIcon = {
            IconButton(
                onClick = {
                    if (!context.openExternalCamera()) {
                        coroutineScope.launch { snackbarHostState.showSnackbar(stringResource(CommonR.string.no_camera_app)) }
                    }
                },
                modifier = Modifier.testTag("backendConfigCameraButton"),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(CommonR.drawable.ic_qr_code_scanner),
                    contentDescription = stringResource(R.string.content_description_backend_config_camera_button),
                )
            }
        },
        modifier = modifier,
        showTitle = showTitle,
        centerText = centerText,
        verticalArrangement = verticalArrangement,
        errorText = errorText,
        isLoading = isLoading,
    )
}

@Composable
fun BackendConfigSuccessContent(modifier: Modifier = Modifier, onContinue: () -> Unit) {
    com.wire.android.ui.authentication.BackendConfigSuccessContent(
        text = backendConfigSuccessText(),
        successIcon = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_validation_check),
                tint = colorsScheme().positive,
                contentDescription = null,
                modifier = Modifier.size(dimensions().spacing16x),
            )
        },
        onContinue = onContinue,
        modifier = modifier,
    )
}

@Composable
private fun backendConfigText() = BackendConfigText(
    title = stringResource(R.string.missing_backend_config_title),
    description = stringResource(R.string.missing_backend_config_description),
    inputLabel = stringResource(R.string.missing_backend_config_input_label),
    inputPlaceholder = stringResource(R.string.missing_backend_config_input_placeholder),
    setupLabel = stringResource(R.string.missing_backend_config_button_setup),
    continueLabel = stringResource(R.string.label_continue),
    successTitle = stringResource(R.string.backend_config_success_title),
    successDescription = stringResource(R.string.backend_config_success_description),
)

@Composable
private fun backendConfigSuccessText() = BackendConfigText(
    title = "",
    description = "",
    inputLabel = "",
    inputPlaceholder = "",
    setupLabel = "",
    continueLabel = stringResource(R.string.label_continue),
    successTitle = stringResource(R.string.backend_config_success_title),
    successDescription = stringResource(R.string.backend_config_success_description),
)
