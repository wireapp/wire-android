package com.wire.android.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination
import com.datadog.android.compose.ExperimentalTrackingApi
import com.datadog.android.compose.NavigationViewTrackingEffect
import com.datadog.android.rum.tracking.AcceptAllNavDestinations
import com.google.accompanist.navigation.animation.rememberAnimatedNavController

@OptIn(ExperimentalAnimationApi::class, ExperimentalTrackingApi::class)
@Composable
fun rememberTrackingAnimatedNavController(nameFromRoute: (String) -> String?) = rememberAnimatedNavController().apply {
    NavigationViewTrackingEffect(
        navController = this,
        trackArguments = true,
        destinationPredicate = object : AcceptAllNavDestinations() {
            override fun getViewName(component: NavDestination): String? = component.route?.let { nameFromRoute(it) }
        }
    )
}
