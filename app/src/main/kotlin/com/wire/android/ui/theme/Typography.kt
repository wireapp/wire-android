package com.wire.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

val Typography.title01: TextStyle
    get() = WireTypography.Title01
val Typography.title02: TextStyle
    get() = WireTypography.Title02
val Typography.title03: TextStyle
    get() = WireTypography.Title03
val Typography.title04: TextStyle
    get() = WireTypography.Title04
val Typography.body01: TextStyle
    get() = WireTypography.Body01
val Typography.body02: TextStyle
    get() = WireTypography.Body02
val Typography.body03: TextStyle
    get() = WireTypography.Body03
val Typography.body04: TextStyle
    get() = WireTypography.Body04
val Typography.subline01: TextStyle
    get() = WireTypography.SubLine01
val Typography.button01: TextStyle
    get() = WireTypography.Button01
val Typography.button02: TextStyle
    get() = WireTypography.Button02
val Typography.button03: TextStyle
    get() = WireTypography.Button03
val Typography.button04: TextStyle
    get() = WireTypography.Button04
val Typography.button05: TextStyle
    get() = WireTypography.Button05
val Typography.label01: TextStyle
    get() = WireTypography.Label01
val Typography.label02: TextStyle
    get() = WireTypography.Label02
val Typography.label03: TextStyle
    get() = WireTypography.Label03
val Typography.label04: TextStyle
    get() = WireTypography.Label04
val Typography.badge01: TextStyle
    get() = WireTypography.Badge01

object WireTypography {
    private val Roboto = FontFamily.Default
    val Title01 = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W500,
        fontStyle = FontStyle.Normal,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        textAlign = TextAlign.Center
    )
    val Title02 = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W500,
        fontStyle = FontStyle.Normal,
        fontSize = 16.sp,
        lineHeight = 20.sp
    )
    val Title03 = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W500,
        fontStyle = FontStyle.Normal,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
    val Title04 = Title01.copy(
        fontWeight = FontWeight.W400,
    )
    val Body01 = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontStyle = FontStyle.Normal,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.05.sp
    )
    val Body02 = Body01.copy(
        fontWeight = FontWeight.W500,
    )
    val Body03 = Body01.copy(
        fontWeight = FontWeight.W700,
    )
    val Body04 = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W700,
        fontStyle = FontStyle.Normal,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.35.sp
    )
    val SubLine01 = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Normal,
        fontSize = 12.sp,
        lineHeight = 14.sp,
    )
    val Button01 = TextStyle(
        fontFamily = Roboto,
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 18.sp
    )
    val Button02 = TextStyle(
        fontFamily = Roboto,
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.W500,
        fontSize = 15.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.3.sp,
        textAlign = TextAlign.Center
    )
    val Button03 = TextStyle(
        fontFamily = Roboto,
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.W500,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
        textAlign = TextAlign.Center
    )
    val Button04 = Button03.copy(
        textDecoration = TextDecoration.Underline
    )
    val Button05 = TextStyle(
        fontFamily = Roboto,
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp,
        textAlign = TextAlign.Center
    )
    val Label01 = TextStyle(
        fontFamily = Roboto,
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.25.sp
    )
    val Label02 = Label01.copy(
        fontWeight = FontWeight.W700,
    )
    val Label03 = TextStyle(
        fontFamily = Roboto,
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.5.sp
    )
    val Label04 = TextStyle(
        fontFamily = Roboto,
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        textAlign = TextAlign.Center
    )
    val Badge01 = TextStyle(
        fontFamily = Roboto,
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.W700,
        fontSize = 10.sp,
        lineHeight = 11.72.sp,
        textAlign = TextAlign.Center
    )
}
