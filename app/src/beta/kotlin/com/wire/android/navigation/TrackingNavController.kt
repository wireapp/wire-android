package com.wire.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination
import androidx.navigation.compose.rememberNavController
import com.datadog.android.compose.ExperimentalTrackingApi
import com.datadog.android.compose.NavigationViewTrackingEffect
import com.datadog.android.rum.tracking.AcceptAllNavDestinations

@OptIn(ExperimentalTrackingApi::class)
@Composable
fun rememberTrackingAnimatedNavController(nameFromRoute: (String) -> String?) = rememberNavController().apply {
    NavigationViewTrackingEffect(
        navController = this,
        trackArguments = true,
        destinationPredicate = object : AcceptAllNavDestinations() {
            override fun getViewName(component: NavDestination): String? = component.route?.let { nameFromRoute(it) }
        }
    )
}
