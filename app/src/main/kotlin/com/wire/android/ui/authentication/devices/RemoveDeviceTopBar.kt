package com.wire.android.ui.authentication.devices

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun RemoveDeviceTopBar(elevation: Dp, onBackNavigationPressed: () -> Unit) {
    WireCenterAlignedTopAppBar(
        elevation = elevation,
        title = stringResource(R.string.remove_device_title),
        onNavigationPressed = onBackNavigationPressed,
    ) {
        Text(
            text = stringResource(id = R.string.remove_device_message),
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.onBackground,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.wireDimensions.removeDeviceHorizontalPadding,
                vertical = MaterialTheme.wireDimensions.removeDeviceMessageVerticalPadding
            )
        )
        Text(
            text = stringResource(id = R.string.remove_device_label),
            style = MaterialTheme.wireTypography.title03,
            color = MaterialTheme.wireColorScheme.labelText,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.wireDimensions.removeDeviceHorizontalPadding,
                vertical = MaterialTheme.wireDimensions.removeDeviceLabelVerticalPadding
            )
        )
    }
}

@Preview(showBackground = false)
@Composable
private fun LoginTopBarPreview() {
    RemoveDeviceTopBar(0.dp) {}
}
