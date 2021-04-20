package com.wire.android.shared.asset.ui.imageloader

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import com.wire.android.core.config.LocaleConfig
import com.wire.android.core.extension.toStringOrEmpty
import com.wire.android.core.ui.drawable.TextDrawable
import com.wire.android.shared.asset.Asset

class IconLoader(private val localeConfig: LocaleConfig) {

    fun load(
        profilePicture: Asset?,
        name: String,
        imageView: ImageView,
        isCirclePlaceholder : Boolean = false,
        requestOptions: RequestOptions.() -> Unit = {}
    ): RequestBuilder<Drawable> {
        val fallback = createFallbackDrawable(name, isCirclePlaceholder)
        val data = profilePicture ?: fallback

        return Glide.with(imageView)
            .load(data)
            .apply(
                RequestOptions()
                    .placeholder(fallback)
                    .error(fallback)
                    .apply { requestOptions(this) }
            )
    }

    private fun createFallbackDrawable(name: String, isCirclePlaceholder : Boolean): Drawable {
        val nameInitial =
            name.firstOrNull().toStringOrEmpty().toUpperCase(localeConfig.currentLocale())
        return TextDrawable(text = nameInitial, isCirclePlaceholder = isCirclePlaceholder)
    }

}
