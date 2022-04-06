package com.wire.android.ui.authentication.login

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.authentication.login.email.LoginEmailScreen
import com.wire.android.ui.common.appBarElevation
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import com.wire.kalium.logic.configuration.ServerConfig

@ExperimentalMaterialApi
@Composable
fun LoginScreen(serverConfig: ServerConfig) {
    val loginViewModel: LoginViewModel = hiltViewModel()
    LoginContent(
        onBackPressed = { loginViewModel.navigateBack() },
        //todo: temporary to show the remoteConfig
        serverTitle = serverConfig.title,
        serverConfig = serverConfig
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LoginContent(
    onBackPressed: () -> Unit,
    serverTitle: String,
    serverConfig: ServerConfig
) {
    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.appBarElevation(),
                title = "${stringResource(R.string.login_title)} $serverTitle",
                onNavigationPressed = onBackPressed
            )
        },
        modifier = Modifier.fillMaxHeight(),
    ) {
        LoginEmailScreen(serverConfig = serverConfig, scrollState = scrollState)
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    WireTheme(isPreview = true) {
        LoginContent(onBackPressed = { }, serverTitle = "", serverConfig = ServerConfig.STAGING)
    }
}

