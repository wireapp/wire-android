package com.wire.android.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


val Typography.subLine1: TextStyle
    get() = TextStyle(
        color = WireColor.Dark70Gray,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Normal,
        fontSize = 12.sp,
        lineHeight = 14.sp,
    )

val Typography.label3: TextStyle
    get() = TextStyle(
        color = WireColor.Dark70Gray,
        fontWeight = FontWeight.W500,
        fontStyle = FontStyle.Normal,
        fontSize = 11.sp,
        lineHeight = 12.sp,
    )

val Typography.title3: TextStyle
    get() = TextStyle(
        color = WireColor.Dark80Gray,
        fontWeight = FontWeight.W500,
        fontStyle = FontStyle.Normal,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )

@Suppress("LongParameterList")

val typography = typographyFromDefaults(
    body2 = TextStyle(
        color = Color.Black,
        fontWeight = FontWeight.W500,
        fontStyle = FontStyle.Normal,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.05.sp
    )
)

fun typographyFromDefaults(
    h1: TextStyle? = null,
    h2: TextStyle? = null,
    h3: TextStyle? = null,
    h4: TextStyle? = null,
    h5: TextStyle? = null,
    h6: TextStyle? = null,
    subtitle1: TextStyle? = null,
    subtitle2: TextStyle? = null,
    body1: TextStyle? = null,
    body2: TextStyle? = null,
    button: TextStyle? = null,
    caption: TextStyle? = null,
    overline: TextStyle? = null
): Typography {
    val defaults = Typography()
    return Typography(
        h1 = defaults.h1.merge(h1),
        h2 = defaults.h2.merge(h2),
        h3 = defaults.h3.merge(h3),
        h4 = defaults.h4.merge(h4),
        h5 = defaults.h5.merge(h5),
        h6 = defaults.h6.merge(h6),
        subtitle1 = defaults.subtitle1.merge(subtitle1),
        subtitle2 = defaults.subtitle2.merge(subtitle2),
        body1 = defaults.body1.merge(body1),
        body2 = defaults.body2.merge(body2),
        button = defaults.button.merge(button),
        caption = defaults.caption.merge(caption),
        overline = defaults.overline.merge(overline)
    )
}
