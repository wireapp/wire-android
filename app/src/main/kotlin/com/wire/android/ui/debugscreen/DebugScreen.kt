package com.wire.android.ui.debugscreen

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.ui.common.topappbar.WireTopAppBarTitle
import com.wire.android.ui.common.topappbar.wireTopAppBarColors
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.startFileShareIntent

@Composable
fun DebugScreen() {
    val debugScreenViewModel: DebugScreenViewModel = hiltViewModel()
    Column {
        TopBar(title = "Debug")
        ListWithHeader("MLS Data") { debugScreenViewModel.mlsData.map { TextRowItem(it) } }
        ListWithHeader("Logs") { LoggingSection(debugScreenViewModel) }
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
    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Text(
            text = text,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.padding(12.dp).weight(1f),
            textAlign = TextAlign.Left,
            fontSize = 14.sp
        )
        trailingIcon?.let {
            Icon(
                painter = painterResource(id = trailingIcon),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.defaultMinSize(80.dp).clickable { onIconClick() }
            )
        }

    }
}

@Composable
fun LoggingSection(debugScreenViewModel: DebugScreenViewModel) {
    val context = LocalContext.current
    val absolutePath = context.cacheDir.absolutePath
    SwitchRowItem(
        text = "Enable Logging", checked = debugScreenViewModel.isLoggingEnabled
    ) { state: Boolean ->
        debugScreenViewModel.setLoggingEnabledState(state, absolutePath)
    }
    TextRowItem(
        "Share Logs",
        trailingIcon = android.R.drawable.ic_menu_share
    ) { context.startFileShareIntent(debugScreenViewModel.logFilePath(absolutePath)) }
}

@Composable
fun SwitchRowItem(
    text: String, checked: Boolean = false, onCheckedChange: ((Boolean) -> Unit)?
) {
    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Text(
            text = text,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.padding(12.dp).weight(1f),
            textAlign = TextAlign.Left,
            fontSize = 14.sp
        )
        Switch(
            modifier = Modifier.defaultMinSize(80.dp),
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Preview(showBackground = false)
@Composable
fun debugScreenPreview() {
    DebugScreen()
}
