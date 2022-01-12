package com.wire.android.core.ui.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.wire.android.core.extension.replaceFragment
import org.koin.android.ext.android.get
import org.koin.core.qualifier.TypeQualifier

class FragmentStackHandler {

    inline fun <reified T : Fragment> replaceFragment(
        activity: FragmentActivity,
        addToBackStack: Boolean = true,
        fragmentBuilder: () -> T
    ) = with(activity) {

        val fragment = supportFragmentManager.findFragmentByTag(T::class.java.canonicalName) ?: fragmentBuilder()
        val containerId = get<FragmentContainerProvider>(TypeQualifier(this::class)).getContainerResId(fragment)

        supportFragmentManager.replaceFragment(containerId, fragment, addToBackStack)
    }


    inline fun <reified T : Fragment> replaceChildFragment(
        parent: Fragment,
        addToBackStack: Boolean = true,
        childFragmentBuilder: () -> T
    ) = with(parent) {

        val child = childFragmentManager.findFragmentByTag(T::class.java.canonicalName) ?: childFragmentBuilder()
        val containerId = get<FragmentContainerProvider>(TypeQualifier(this::class)).getContainerResId(child)

        childFragmentManager.replaceFragment(containerId, child, addToBackStack)
    }
}
