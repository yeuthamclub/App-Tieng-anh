package com.yeuthamclub.tienganh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DEPTH = 4.dp

/** A chunky button whose face sinks into its shade while pressed. */
@Composable
fun DuoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Duo.Green,
    shade: Color = Duo.GreenShade,
    textColor: Color = Duo.White,
    enabled: Boolean = true,
) {
    Pressable(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (enabled) color else Duo.Gray,
        shade = if (enabled) shade else Duo.GrayShade,
        enabled = enabled,
    ) {
        Text(
            text.uppercase(),
            color = if (enabled) textColor else Duo.TextSoft,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            letterSpacing = 0.8.sp,
        )
    }
}

/** A round "3D" button, used for path nodes, the speaker and the microphone. */
@Composable
fun DuoCircle(
    size: Dp,
    color: Color,
    shade: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Pressable(onClick, modifier.size(size), CircleShape, color, shade, enabled, content)
}

@Composable
private fun Pressable(
    onClick: () -> Unit,
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape,
    color: Color,
    shade: Color,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .clip(shape)
            .background(shade)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(bottom = DEPTH)
                .offset(y = if (pressed) DEPTH else 0.dp)
                .clip(shape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

/** An answer choice: white card with a gray rim, blue when selected, green/red after checking. */
@Composable
fun OptionCard(text: String, state: OptionState, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val (bg, rim, fg) = when (state) {
        OptionState.Idle -> Triple(Duo.White, Duo.Gray, Duo.Text)
        OptionState.Selected -> Triple(Duo.BlueLight, Duo.Blue, Duo.BlueShade)
        OptionState.Right -> Triple(Duo.GreenLight, Duo.Green, Duo.GreenShade)
        OptionState.Wrong -> Triple(Duo.RedLight, Duo.Red, Duo.RedShade)
    }
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rim)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(bottom = DEPTH)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .border(2.dp, rim, RoundedCornerShape(16.dp))
                .padding(vertical = 16.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

enum class OptionState { Idle, Selected, Right, Wrong }

/** Rounded green progress bar like the one on top of a Duolingo lesson. */
@Composable
fun DuoProgress(fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier.height(16.dp).clip(RoundedCornerShape(8.dp)).background(Duo.Gray)) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(Duo.Green),
        )
    }
}
