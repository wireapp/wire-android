package com.wire.android.core.ui.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

class ViewHolderInflater {

    fun inflate(@LayoutRes resource: Int, parent: ViewGroup): View =
        LayoutInflater.from(parent.context).inflate(resource, parent, false)
}
