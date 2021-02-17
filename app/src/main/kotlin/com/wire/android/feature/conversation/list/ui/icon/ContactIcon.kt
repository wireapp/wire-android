package com.wire.android.feature.conversation.list.ui.icon

import android.content.Context
import android.graphics.drawable.Drawable
import com.wire.android.core.config.LocaleConfig
import com.wire.android.core.extension.toStringOrEmpty
import com.wire.android.core.ui.drawable.TextDrawable
import java.io.File

/**
 * Type [T] should be one of the types supported by image loading library
 */
interface ContactIcon<T> {
    fun create(context: Context, width: Int, height: Int): T
}

class ContactProfilePictureIcon(private val filePath: String) : ContactIcon<File> {

    override fun create(context: Context, width: Int, height: Int): File = File(filePath)
}

class ContactNameInitialIcon(contactName: String, localeConfig: LocaleConfig) : ContactIcon<Drawable> {

    private val nameInitial = contactName.firstOrNull().toStringOrEmpty().toUpperCase(localeConfig.currentLocale())

    override fun create(context: Context, width: Int, height: Int): Drawable =
        TextDrawable(text = nameInitial, width = width.toFloat(), height = height.toFloat())
}
