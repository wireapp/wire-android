package com.wire.android.core.extension

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) =
    beginTransaction().func().commit()

//TODO: Fragment shouldn't be aware of activity's view ids
fun Fragment.replaceFragment(frameId: Int, fragment: Fragment, addToBackStack: Boolean = true) {
    (activity as AppCompatActivity).replaceFragment(frameId, fragment, addToBackStack)
}

//TODO is this the best approach?
fun Fragment.openUrl(url: String) =
    requireActivity().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

fun Fragment.showKeyboard() = activity?.showKeyboard()

@Suppress("SpreadOperator")
fun <T : Fragment> T.withArgs(vararg args: Pair<String, Any?>): T = this.apply {
    arguments = bundleOf(*args)
}
