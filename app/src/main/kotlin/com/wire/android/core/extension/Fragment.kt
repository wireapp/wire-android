package com.wire.android.core.extension

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

inline fun FragmentManager.doTransaction(func: FragmentTransaction.() -> FragmentTransaction) =
    beginTransaction().func().commit()

//TODO: Fragment shouldn't be aware of activity's view ids
fun Fragment.replaceFragment(frameId: Int, fragment: Fragment, addToBackStack: Boolean = true) {
    (activity as AppCompatActivity).replaceFragment(frameId, fragment, addToBackStack)
}
