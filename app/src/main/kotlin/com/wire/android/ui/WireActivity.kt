package com.wire.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.ExperimentalComposeUiApi
import com.wire.android.ui.theme.WireTheme

@ExperimentalAnimationApi
@ExperimentalComposeUiApi
class WireActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WireTheme {
                WireApp()
            }
        }
    }

}

