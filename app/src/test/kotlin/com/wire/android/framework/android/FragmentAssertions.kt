package com.wire.android.framework.android

import android.os.Bundle
import androidx.fragment.app.Fragment

fun Fragment.argumentEquals(other: Fragment): Boolean {
    val thisArg = this.arguments
    val otherArg = other.arguments

    return if (thisArg == null && otherArg == null) true
    else if (thisArg != null && otherArg != null) thisArg.contentEquals(otherArg)
    else false
}

fun Bundle.contentEquals(other: Bundle): Boolean {

    fun printError() = println("Contents aren't equal for Bundles \nthis: $this\nother: $other")

    if (this.size() != other.size()) return false

    val keySet = this.keySet()
    if (!other.keySet().containsAll(keySet)) return false

    keySet.forEach {
        val thisValue = this[it]
        val otherValue = other[it]

        if (thisValue is Bundle && otherValue is Bundle) {
            if (!thisValue.contentEquals(otherValue)) {
                printError()
                return false
            }
        } else if (thisValue is Array<*> && otherValue is Array<*>) {
            if (!thisValue.contentEquals(otherValue)) {
                printError()
                return false
            }
        } else if (other[it] != this[it]) {
            printError()
            return false
        }
    }
    return true
}
