package com.wire.android.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.kaliumFileWriter
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.startFileShareIntent
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logic.CoreLogger
import java.io.File


// the logic here is just temporary and will be updated to be added to the VM once we build the settings screen
@Composable
fun SettingsScreen() {
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ) {
        val checkedState = remember { mutableStateOf(settingsViewModel.isLoggingEnabled()) }

        Text(
            text = "Enable logging",
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.wireDimensions.spacing16x)
        )

        Switch(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.wireDimensions.spacing16x),
            checked = checkedState.value,
            onCheckedChange = {
                checkedState.value = it
                settingsViewModel.enableLogging(it)

                if (it) {
                    kaliumFileWriter.init(context.cacheDir.absolutePath)
                    CoreLogger.setLoggingLevel(
                        level = KaliumLogLevel.DEBUG, kaliumFileWriter
                    )
                } else {
                    kaliumFileWriter.clearFileContent(
                        File(context.cacheDir.absolutePath + "/logs/" + "wire_logs.log")
                    )
                    CoreLogger.setLoggingLevel(
                        level = KaliumLogLevel.DISABLED, kaliumFileWriter
                    )
                }
            }
        )
//        state = if (checkedState.value) WireButtonState.Default else WireButtonState.Disabled,

        WirePrimaryButton(
            text = "Share the log",
            onClick = { context.startFileShareIntent(context.cacheDir.absolutePath + "/logs/" + "wire_logs.log") },
            fillMaxWidth = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.wireDimensions.spacing16x)
        )


        Text(
            text = "Settings is under construction",
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            textAlign = TextAlign.Center,
            fontSize = 18.sp
        )
    }

}

@Preview(showBackground = false)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
