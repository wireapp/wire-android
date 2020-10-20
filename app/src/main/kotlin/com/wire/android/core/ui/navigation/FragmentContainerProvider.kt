package com.wire.android.core.ui.navigation

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment

@Suppress("UnnecessaryAbstractClass")
abstract class FragmentContainerProvider {
    @IdRes
    abstract fun getContainerResId(fragment: Fragment): Int

    companion object {
        fun fixedProvider(@IdRes containerResId: Int) = object: FragmentContainerProvider() {
            override fun getContainerResId(fragment: Fragment): Int = containerResId
        }
    }
}
