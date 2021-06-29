package com.wire.android.core.extension

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide

fun ImageView.clear() {
    Glide.with(this).clear(this)
}

fun ImageView.load(drawable: Drawable) {
    Glide.with(this).load(drawable).into(this)
}

fun ImageView.load(@DrawableRes drawableRes: Int) {
    Glide.with(this).load(drawableRes).into(this)
}
