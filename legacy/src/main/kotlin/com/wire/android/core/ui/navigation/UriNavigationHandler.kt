package com.wire.android.core.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri

class UriNavigationHandler {

    fun openUri(context: Context, uriString: String) = openUri(context, Uri.parse(uriString))

    fun openUri(context: Context, uri: Uri) = context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}
