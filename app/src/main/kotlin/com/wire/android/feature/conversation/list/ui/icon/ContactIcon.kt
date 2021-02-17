package com.wire.android.feature.conversation.list.ui.icon

import android.content.Context
import com.wire.android.core.config.LocaleConfig
import com.wire.android.core.extension.toStringOrEmpty
import com.wire.android.core.ui.drawable.TextDrawable
import java.io.File

interface ContactIcon {
    fun create(context: Context, width: Int, height: Int): Any?
}

class ContactProfilePictureIcon(private val filePath: String) : ContactIcon {

    override fun create(context: Context, width: Int, height: Int) = File(filePath)
}

class ContactNameInitialIcon(contactName: String, localeConfig: LocaleConfig) : ContactIcon {

    private val nameInitial = contactName.firstOrNull().toStringOrEmpty().toUpperCase(localeConfig.currentLocale())

    override fun create(context: Context, width: Int, height: Int) =
        TextDrawable(text = nameInitial, width = width.toFloat(), height = height.toFloat())
}
