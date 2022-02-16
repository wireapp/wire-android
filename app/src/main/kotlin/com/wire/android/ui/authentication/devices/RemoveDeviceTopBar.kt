package com.wire.android.ui.authentication.devices

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.BackNavigationIconButton
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@Composable
fun RemoveDeviceTopBar(onBackNavigationPressed: () -> Unit) {
    Column {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.remove_device_title),
                    style = MaterialTheme.wireTypography.title01
                )
            },
            navigationIcon = { BackNavigationIconButton { onBackNavigationPressed() } },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            )
        )
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
    RemoveDeviceTopBar {}
}
