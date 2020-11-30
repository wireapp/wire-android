package com.wire.android.core.extension

import android.view.View
import androidx.recyclerview.widget.RecyclerView

fun <T: View>  RecyclerView.ViewHolder.lazyFind(viewId: Int): Lazy<T> = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    itemView.findViewById(viewId)
}
