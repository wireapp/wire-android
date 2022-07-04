package com.wire.android.ui.debugscreen

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.WireSwitch
import com.wire.android.ui.common.topappbar.WireTopAppBarTitle
import com.wire.android.ui.common.topappbar.wireTopAppBarColors
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.getDeviceId
import com.wire.android.util.startMultipleFileSharingIntent

@Composable
fun DebugScreen() {
    val debugScreenViewModel: DebugScreenViewModel = hiltViewModel()
    DebugContent(
        mlsData = debugScreenViewModel.mlsData,
        isLoggingEnabled = debugScreenViewModel.isLoggingEnabled,
        setLoggingEnabledState = debugScreenViewModel::setLoggingEnabledState,
        logFilePath = debugScreenViewModel::logFilePath,
        deleteAllLogs = debugScreenViewModel::deleteAllLogs
    )
}

@Composable
fun DebugContent(
    mlsData: List<String>,
    isLoggingEnabled: Boolean,
    setLoggingEnabledState: (Boolean) -> Unit,
    logFilePath: () -> String,
    deleteAllLogs: () -> Unit
) {
    Column {
        TopBar(title = "Debug")
        ListWithHeader("MLS Data") {
            mlsData.map { TextRowItem(it) }
        }
        ListWithHeader("Logs") {
            LoggingSection(isLoggingEnabled, setLoggingEnabledState, logFilePath, deleteAllLogs)
        }
    }
}

@Composable
fun TopBar(title: String) {
    val colors = wireTopAppBarColors()
    Surface(
        shadowElevation = MaterialTheme.wireDimensions.topBarShadowElevation,
        color = colors.containerColor(scrollFraction = 0f).value
    ) {
        CenterAlignedTopAppBar(
            title = { WireTopAppBarTitle(title = title, MaterialTheme.wireTypography.title01) },
            colors = colors,
        )
    }
}

@Composable
fun ListWithHeader(
    headerTitle: String, content: @Composable () -> Unit = {}
) {
    Column {
        FolderHeader(headerTitle)
        content()
    }
}

@Composable
fun TextRowItem(text: String, @DrawableRes trailingIcon: Int? = null, onIconClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
            modifier = Modifier
                .padding(10.dp)
                .weight(1f),
            textAlign = TextAlign.Left,
            fontSize = 14.sp
        )
        trailingIcon?.let {
            Icon(
                painter = painterResource(id = trailingIcon),
                contentDescription = "",
                tint = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
                modifier = Modifier
                    .defaultMinSize(80.dp)
                    .clickable { onIconClick() }
            )
        }
    }
}

@Composable
fun LoggingSection(
    isLoggingEnabled: Boolean,
    setLoggingEnabledState: (Boolean) -> Unit,
    logFilePath: () -> String,
    deleteAllLogs: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val absolutePath = context.cacheDir?.absolutePath ?: ""
    SwitchRowItem(
        text = "Enable Logging", checked = isLoggingEnabled
    ) { state: Boolean ->
        setLoggingEnabledState(state)
    }
    TextRowItem(
        "Share Logs",
        trailingIcon = android.R.drawable.ic_menu_share
    ) { context.startMultipleFileSharingIntent(logFilePath()) }

    TextRowItem(
        "Delete All Logs",
        trailingIcon = android.R.drawable.ic_delete
    ) { deleteAllLogs() }

    TextRowItem(
        "Device id : ${getDeviceId(context)}",
        trailingIcon = R.drawable.ic_copy
    ) {
        getDeviceId(context)?.let { AnnotatedString(it) }?.let {
            clipboardManager.setText(it)
            Toast.makeText(context, "Text Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun SwitchRowItem(
    text: String, checked: Boolean = false, onCheckedChange: ((Boolean) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
            modifier = Modifier
                .padding(10.dp)
                .weight(1f),
            textAlign = TextAlign.Left,
            fontSize = 14.sp
        )
        WireSwitch(
            modifier = Modifier.padding(end = 20.dp),
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Preview(showBackground = false)
@Composable
fun debugScreenPreview() {
    DebugContent(listOf(), true, { _: Boolean, _: String -> }, { "" }, {})
}
