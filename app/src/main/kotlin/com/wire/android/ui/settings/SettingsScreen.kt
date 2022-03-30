package com.wire.android.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.notification.MessageNotificationManager
import com.wire.android.ui.common.UnderConstructionScreen
import com.wire.android.ui.common.button.WireSecondaryButton

@Composable
fun SettingsScreen() {
    UnderConstructionScreen(screenName = "SettingsScreen")
    TestNotificationsView()
}

@Composable
private fun TestNotificationsView() {
    val context = LocalContext.current

    //TODO remove it
    Column(modifier = Modifier.padding(32.dp)) {

        WireSecondaryButton(text = "test notification: few conversations",
            modifier = Modifier.padding(bottom = 32.dp),
            onClick = {
                MessageNotificationManager(context).testIt()
            })

        WireSecondaryButton(text = "test notification: single message",
            modifier = Modifier.padding(bottom = 32.dp),
            onClick = {
                MessageNotificationManager(context).testIt2()
            })
    }
}

@Preview(showBackground = false)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
