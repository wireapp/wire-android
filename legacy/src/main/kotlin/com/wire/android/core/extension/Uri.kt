package com.wire.android.core.extension

import android.net.Uri

private const val SCHEME_DELIMITER = "://"

fun Uri.Builder.domainAddress(address: String) = this.apply {
    val (scheme, authority) = address.split(SCHEME_DELIMITER)
    scheme(scheme).authority(authority)
}
