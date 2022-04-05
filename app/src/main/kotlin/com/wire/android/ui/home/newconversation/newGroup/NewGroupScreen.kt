package com.wire.android.ui.home.newconversation.newGroup


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupScreen() {

    NewGroupScreenContent()
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class
)
@Composable
fun NewGroupScreenContent() {

    Scaffold(topBar = {
        WireCenterAlignedTopAppBar(
            elevation = 0.dp,
            title = stringResource(id = R.string.new_group_title))

    }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top) {
            val keyboardController = LocalSoftwareKeyboardController.current
            Text(
                text = stringResource(id = R.string.new_group_description),
                style = MaterialTheme.wireTypography.body01,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.wireDimensions.spacing16x,
                        vertical = MaterialTheme.wireDimensions.spacing24x
                    )
            )
        }
    }
}
