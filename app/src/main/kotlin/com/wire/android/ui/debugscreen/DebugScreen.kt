package com.wire.android.ui.debugscreen

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
import androidx.compose.ui.graphics.Color
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
        topBar("Debug")
        list("MLS Data") { debugScreenViewModel.mlsData.map { textRowItem(it) } }
        list("Logs") { debugLog(debugScreenViewModel) }
    }
}

@Composable
fun topBar(title: String) {
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
fun list(
    headerTitle: String, content: @Composable () -> Unit = {}
) {
    Column {
        FolderHeader(headerTitle)
        content()
    }
}

@Composable
fun textRowItem(text: String, trailingIcon: Int? = null, onIconClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Text(
            text = text,
            fontWeight = FontWeight.Normal,
            color = Color.DarkGray,
            modifier = Modifier.padding(12.dp).weight(1f),
            textAlign = TextAlign.Left,
            fontSize = 14.sp
        )
        trailingIcon?.let {
            Icon(painter = painterResource(id = trailingIcon),
                contentDescription = "",
                tint = Color.Black,
                modifier = Modifier.defaultMinSize(80.dp).clickable { onIconClick() }
            )
        }

    }
}

@Composable
fun debugLog(debugScreenViewModel: DebugScreenViewModel) {
    val context = LocalContext.current
    val absolutePath = context.cacheDir.absolutePath
    switchRowItem(
        text = "Enable Logging", checked = debugScreenViewModel.checkedState.value
    ) { state: Boolean ->
        debugScreenViewModel.setLoggingEnabledState(state, absolutePath)
    }
    textRowItem(
        "Share Logs",
        trailingIcon = android.R.drawable.ic_menu_share
    ) { context.startFileShareIntent(debugScreenViewModel.logFilePath(absolutePath)) }
}

@Composable
fun switchRowItem(
    text: String, checked: Boolean = false, onCheckedChange: ((Boolean) -> Unit)?
) {
    Row(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Text(
            text = text,
            fontWeight = FontWeight.Normal,
            color = Color.DarkGray,
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
