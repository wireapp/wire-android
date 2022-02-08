package com.wire.android.util

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.wire.android.R

// TODO: maybe change name to something better, like: IntentHelper? IntentUtil? BrowserIntentNavigator?
object CustomTabsHelper {

    @JvmStatic
    fun launchUrl(context: Context, url: String) {
        // TODO: add verification if can launch CCT otherwise fallback to Intent.ACTION_VIEW
        val builder = CustomTabsIntent.Builder()
        val color = ContextCompat.getColor(context, R.color.background) // TODO: get color from material without making it composable
        val colors = CustomTabColorSchemeParams.Builder()
            .setNavigationBarColor(color)
            .setToolbarColor(color)
            .build()
        builder.setDefaultColorSchemeParams(colors)
        builder.setCloseButtonIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_close))
        builder.setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        builder.setShowTitle(true)

        val customTabsIntent = builder.build()
        customTabsIntent.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + context.packageName))
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }
}
