package com.wire.android.core.extension

import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) =
    beginTransaction().func().commit()

fun FragmentManager.replaceFragment(frameId: Int, fragment: Fragment, addToBackStack: Boolean = true) =
    inTransaction {
        replace(frameId, fragment).also {
            if (addToBackStack) {
                it.addToBackStack(fragment.tag)
            }
        }
    }

/**
 * @see [Activity.showKeyboard]
 */
fun Fragment.showKeyboard() {
    activity?.showKeyboard()
}

/**
 * Focuses on the [view] and opens the keyboard for input
 *
 * @param view to request focus and receive the keyboard input
 */
fun Fragment.showKeyboardWithFocusOn(view: View) {
    view.requestFocus()
    showKeyboard()
}

@Suppress("SpreadOperator")
fun <T : Fragment> T.withArgs(vararg args: Pair<String, Any?>): T = this.apply {
    arguments = bundleOf(*args)
}
