package com.wire.android.ui.home.settings.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAccountScreen() {
    Scaffold(topBar = {
        WireCenterAlignedTopAppBar(
            onNavigationPressed = {},
            elevation = 0.dp,
            title = stringResource(id = R.string.settings_your_account_label)
        )
    }) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            Text(text = "holis!")
        }
    }
}

