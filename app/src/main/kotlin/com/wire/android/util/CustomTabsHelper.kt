package com.wire.android.util

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.wire.android.R

object CustomTabsHelper {

    @JvmStatic
    fun launchUrl(context: Context, url: String) {
        val builder = CustomTabsIntent.Builder()
            .setCloseButtonIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_close))
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .setShowTitle(true)

        val customTabsIntent = builder.build()
        customTabsIntent.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + context.packageName))
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }
}
