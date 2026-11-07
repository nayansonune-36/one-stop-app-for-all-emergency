package com.sentinel.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Android's system SansSerif defaults to Roboto/Noto Sans, which provides native Unicode support
// for Devanagari (Hindi) and Latin characters.
val SentinelFontFamily = FontFamily.SansSerif

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = SentinelFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.04).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = SentinelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.03).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SentinelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.02).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SentinelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.01).sp
    ),
    titleLarge = TextStyle(
        fontFamily = SentinelFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SentinelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = SentinelFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SentinelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SentinelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SentinelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SentinelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SentinelFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.05.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SentinelFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 1.1.sp // Eyebrow uppercase letter spacing
    )
)
