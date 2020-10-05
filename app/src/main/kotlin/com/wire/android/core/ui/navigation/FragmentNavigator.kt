package com.wire.android.core.ui.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.wire.android.core.extension.replaceFragment
import org.koin.android.ext.android.get
import org.koin.core.qualifier.TypeQualifier

class FragmentNavigator {

    fun openFragment(activity: FragmentActivity, fragment: Fragment, addToBackStack: Boolean = true) = with(activity) {
        val containerId = get<FragmentContainerProvider>(TypeQualifier(this::class)).getContainerResId(fragment)
        supportFragmentManager.replaceFragment(containerId, fragment, addToBackStack)
    }

    fun openChildFragment(parent: Fragment, child: Fragment, addToBackStack: Boolean = true) = with(parent) {
        val containerId = get<FragmentContainerProvider>(TypeQualifier(this::class)).getContainerResId(child)
        childFragmentManager.replaceFragment(containerId, child, addToBackStack)
    }
}
