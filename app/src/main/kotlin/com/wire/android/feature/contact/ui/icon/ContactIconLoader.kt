package com.wire.android.feature.contact.ui.icon

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import com.wire.android.core.config.LocaleConfig
import com.wire.android.core.extension.toStringOrEmpty
import com.wire.android.core.ui.drawable.TextDrawable
import com.wire.android.feature.contact.Contact

class ContactIconLoader(private val localeConfig: LocaleConfig) {

    fun load(
        contact: Contact,
        imageView: ImageView,
        requestOptions: RequestOptions.() -> Unit = {}
    ): RequestBuilder<Drawable> {
        val fallback = createFallbackDrawable(contact)
        //TODO load Asset type with Glide
        val data = /* contact.profilePicturePath?.let { File(it) } ?: */ fallback

        return Glide.with(imageView)
            .load(data)
            .apply(RequestOptions()
                .placeholder(fallback)
                .error(fallback)
                .apply { requestOptions(this) }
            )
    }

    private fun createFallbackDrawable(contact: Contact): Drawable {
        val nameInitial = contact.name.firstOrNull().toStringOrEmpty().toUpperCase(localeConfig.currentLocale())
        return TextDrawable(text = nameInitial)
    }
}
