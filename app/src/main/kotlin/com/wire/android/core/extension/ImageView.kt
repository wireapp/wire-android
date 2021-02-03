package com.wire.android.core.extension

import androidx.annotation.DimenRes
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import com.wire.android.R

fun ShapeableImageView.setCircularShape() {
    val shapeModel = ShapeAppearanceModel.builder(
        context,
        R.style.ShapeAppearance_MaterialComponents_Circle,
        0
    ).build()

    shapeAppearanceModel = shapeModel
}

fun ShapeableImageView.setCorneredShape(@DimenRes cornerRadiusResId: Int) {
    val cornerRadius = resources.getDimensionPixelSize(cornerRadiusResId)

    val shapeModel = ShapeAppearanceModel.builder(
        context,
        R.style.ShapeAppearance_MaterialComponents,
        0
    ).setAllCornerSizes(cornerRadius.toFloat())
        .build()

    shapeAppearanceModel = shapeModel
}
