package com.yeuthamclub.tienganh.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Duolingo-like palette: bright green primary, chunky "3D" buttons with a darker shade underneath. */
object Duo {
    val Green = Color(0xFF58CC02)
    val GreenShade = Color(0xFF58A700)
    val GreenLight = Color(0xFFD7FFB8)
    val Blue = Color(0xFF1CB0F6)
    val BlueShade = Color(0xFF1899D6)
    val BlueLight = Color(0xFFDDF4FF)
    val Red = Color(0xFFFF4B4B)
    val RedShade = Color(0xFFEA2B2B)
    val RedLight = Color(0xFFFFDFE0)
    val Gold = Color(0xFFFFC800)
    val GoldShade = Color(0xFFE5B400)
    val Orange = Color(0xFFFF9600)
    val Gray = Color(0xFFE5E5E5)
    val GrayShade = Color(0xFFCECECE)
    val Text = Color(0xFF4B4B4B)
    val TextSoft = Color(0xFF777777)
    val White = Color.White
}

@Composable
fun DuoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Duo.Green,
            onPrimary = Duo.White,
            background = Duo.White,
            surface = Duo.White,
            onBackground = Duo.Text,
            onSurface = Duo.Text,
        ),
        content = content,
    )
}
