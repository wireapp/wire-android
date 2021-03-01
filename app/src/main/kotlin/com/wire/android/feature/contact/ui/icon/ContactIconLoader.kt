package com.wire.android.feature.contact.ui.icon

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import com.wire.android.core.config.LocaleConfig
import com.wire.android.core.extension.toStringOrEmpty
import com.wire.android.core.ui.drawable.TextDrawable
import com.wire.android.feature.contact.Contact
import java.io.File

class ContactIconLoader(private val localeConfig: LocaleConfig, private val appContext: Context) {

    fun load(
        contact: Contact,
        requestOptions: RequestOptions.() -> Unit ={}
    ): RequestBuilder<Drawable> {
        val fallback = createFallbackDrawable(contact)
        val data = contact.profilePicturePath?.let { File(it) } ?: fallback

        return Glide.with(appContext)
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
