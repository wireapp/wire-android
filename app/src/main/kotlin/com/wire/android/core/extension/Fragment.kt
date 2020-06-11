package com.wire.android.core.extension

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

inline fun FragmentManager.doTransaction(func: FragmentTransaction.() -> FragmentTransaction) =
    beginTransaction().func().commit()