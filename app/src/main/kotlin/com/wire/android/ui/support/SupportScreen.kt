package com.wire.android.ui.support

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.common.UnderConstructionScreen

@Composable
fun SupportScreen() {
    UnderConstructionScreen(screenName = "SupportScreen")
}

@Preview(showBackground = false)
@Composable
fun SupportScreenPreview() {
    SupportScreen()
}
