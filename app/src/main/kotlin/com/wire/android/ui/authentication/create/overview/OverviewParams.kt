package com.wire.android.ui.authentication.create.overview

import androidx.annotation.DrawableRes

data class OverviewParams(
    val title: String = "",
    val contentTitle: String = "",
    val contentText: String = "",
    @DrawableRes val contentIconResId: Int = 0,
    val learnMoreText: String = "",
    val learnMoreUrl: String = ""
)
