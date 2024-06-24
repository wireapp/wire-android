package com.wire.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController

@Composable
fun rememberTrackingAnimatedNavController(nameFromRoute: (String) -> String?) =
    rememberNavController()
