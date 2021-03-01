package com.wire.android.feature.contact.ui.icon

import android.graphics.drawable.Drawable
import android.widget.ImageView
import coil.loadAny
import coil.request.ImageRequest
import com.wire.android.core.config.LocaleConfig
import com.wire.android.core.extension.toStringOrEmpty
import com.wire.android.core.ui.drawable.TextDrawable
import com.wire.android.feature.contact.Contact
import java.io.File

class ContactIconLoader(private val localeConfig: LocaleConfig) {

    fun load(
        contact: Contact,
        imageView: ImageView,
        requestBuilder: ImageRequest.Builder.() -> Unit
    ) {
        val fallback = createFallbackDrawable(contact)
        val data = contact.profilePicturePath?.let { File(it) } ?: fallback

        imageView.loadAny(data) {
            placeholder(fallback)
            error(fallback)
            requestBuilder(this)
        }
    }

    private fun createFallbackDrawable(contact: Contact): Drawable {
        val nameInitial = contact.name.firstOrNull().toStringOrEmpty().toUpperCase(localeConfig.currentLocale())
        return TextDrawable(text = nameInitial)
    }
}
