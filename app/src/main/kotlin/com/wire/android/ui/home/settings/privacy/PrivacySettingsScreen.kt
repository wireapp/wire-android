package com.wire.android.ui.home.settings.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversations.details.options.ArrowType
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsItem
import com.wire.android.ui.home.conversations.details.options.SwitchState

@Composable
fun PrivacySettingsConfigScreen() {
    PrivacySettingsScreenContent(
        onBackPressed = { /*TODO*/ },
        isReadReceiptsEnabled = true,
        setReadReceiptsState = {})
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreenContent(
    onBackPressed: () -> Unit,
    isReadReceiptsEnabled: Boolean,
    setReadReceiptsState: (Boolean) -> Unit

) {
    Scaffold(topBar = {
        WireCenterAlignedTopAppBar(
            onNavigationPressed = onBackPressed,
            elevation = 0.dp,
            title = stringResource(id = R.string.settings_privacy_settings_label)
        )
    }) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            GroupConversationOptionsItem(
                title = stringResource(R.string.settings_send_read_receipts),
                switchState = SwitchState.Enabled(value = isReadReceiptsEnabled, onCheckedChange = setReadReceiptsState),
                arrowType = ArrowType.NONE,
                subtitle = stringResource(id = R.string.settings_send_read_receipts_description)
            )
        }
    }
}

@Composable
@Preview
fun PreviewSendReadReceipts() {
    PrivacySettingsScreenContent({}, true, {})
}
