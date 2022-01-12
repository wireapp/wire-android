package com.wire.android.shared.asset.ui.imageloader

import com.bumptech.glide.load.Key
import com.bumptech.glide.load.Options
import java.security.MessageDigest

data class ImageLoaderKey(val uniqueId: String, val width: Int, val height: Int, val options: Options) : Key {

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(toString().toByteArray(Key.CHARSET))
    }
}
