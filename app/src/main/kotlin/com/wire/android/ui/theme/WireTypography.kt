package com.wire.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import io.github.esentsov.PackagePrivate

@Immutable
data class WireTypography(
    val title01: TextStyle,  val title02: TextStyle,  val title03: TextStyle,  val title04: TextStyle,
    val body01: TextStyle,   val body02: TextStyle,   val body03: TextStyle,   val body04: TextStyle,
    val button01: TextStyle, val button02: TextStyle, val button03: TextStyle, val button04: TextStyle, val button05: TextStyle,
    val label01: TextStyle,  val label02: TextStyle,  val label03: TextStyle,  val label04: TextStyle, val label05: TextStyle,
    val badge01: TextStyle,
    val subline01: TextStyle,
    val code01: TextStyle,
) {
    fun toTypography() = Typography(
        titleLarge = title01,  titleMedium = title02, titleSmall = title03,
        labelLarge = button02, labelMedium = label02, labelSmall = label03,
        bodyLarge = body01,    bodyMedium = label04,  bodySmall = subline01
    )
}

private val DefaultWireTypography = WireTypography(
    title01 = WireTypographyBase.Title01,
    title02 = WireTypographyBase.Title02,
    title03 = WireTypographyBase.Title03,
    title04 = WireTypographyBase.Title04,
    body01 = WireTypographyBase.Body01,
    body02 = WireTypographyBase.Body02,
    body03 = WireTypographyBase.Body03,
    body04 = WireTypographyBase.Body04,
    button01 = WireTypographyBase.Button01,
    button02 = WireTypographyBase.Button02,
    button03 = WireTypographyBase.Button03,
    button04 = WireTypographyBase.Button04,
    button05 = WireTypographyBase.Button05,
    label01 = WireTypographyBase.Label01,
    label02 = WireTypographyBase.Label02,
    label03 = WireTypographyBase.Label03,
    label04 = WireTypographyBase.Label04,
    label05 = WireTypographyBase.Label05,
    badge01 = WireTypographyBase.Badge01,
    subline01 = WireTypographyBase.SubLine01,
    code01 = WireTypographyBase.Code01
)

@PackagePrivate
val WireTypographyTypes: ScreenSizeDependent<WireTypography> = ScreenSizeDependent(
    compactPhone = DefaultWireTypography,
    defaultPhone = DefaultWireTypography,
    tablet7 = DefaultWireTypography,
    tablet10 = DefaultWireTypography
)
