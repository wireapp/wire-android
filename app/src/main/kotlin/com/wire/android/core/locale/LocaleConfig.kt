package com.wire.android.core.locale

import android.content.Context
import java.util.Locale

class LocaleConfig(private val appContext: Context) {

    fun currentLocale(): Locale = appContext.applicationContext.resources.configuration.locales[0] ?: Locale.getDefault()
}
