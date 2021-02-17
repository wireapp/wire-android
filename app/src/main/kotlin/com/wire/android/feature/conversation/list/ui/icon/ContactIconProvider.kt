package com.wire.android.feature.conversation.list.ui.icon

import com.wire.android.core.config.LocaleConfig
import com.wire.android.feature.contact.Contact

class ContactIconProvider(private val localeConfig: LocaleConfig) {

    fun provide(contact: Contact): ContactIcon<*> =
        if (contact.profilePicturePath != null) ContactProfilePictureIcon(contact.profilePicturePath)
        else ContactNameInitialIcon(contact.name, localeConfig)
}