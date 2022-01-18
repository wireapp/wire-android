package com.wire.android.ui.main.userprofile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.common.UnderConstructionScreen
import com.wire.kalium.logic.CoreLogic

@Composable
fun UserProfileScreen() {
    UnderConstructionScreen(screenName = "UserProfileScreen")
}

@Preview(showBackground = false)
@Composable
fun UserProfileScreenPreview() {
    val a: CoreLogic
    UserProfileScreen()
}
