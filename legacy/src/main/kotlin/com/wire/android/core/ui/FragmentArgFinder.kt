package com.wire.android.core.ui

import androidx.fragment.app.Fragment
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class FragmentArgFinder<T>(private val key: String) : ReadOnlyProperty<Fragment, T> {
    override fun getValue(thisRef: Fragment, property: KProperty<*>): T = thisRef.requireArguments().get(key) as T
}

fun <T> Fragment.arg(key: String) = FragmentArgFinder<T>(key)
