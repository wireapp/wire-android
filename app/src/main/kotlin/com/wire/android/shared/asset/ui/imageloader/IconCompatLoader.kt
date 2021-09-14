package com.wire.android.shared.asset.ui.imageloader

import android.content.Context
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import com.wire.android.shared.asset.Asset

class IconCompatLoader {

    fun loadIcon(context: Context, asset: Asset?): IconCompat = IconCompat.createWithBitmap(
        Glide.with(context)
            .asBitmap()
            .circleCrop()
            .load(asset)
            .submit(ICON_WIDTH, ICON_HEIGHT)
            .get()
    )

    companion object {
        private const val ICON_WIDTH = 300
        private const val ICON_HEIGHT = 300
    }
}
