package com.kaafisir.auramic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

/** A handful of bars that dance with the live mic [level] (0..1). */
@Composable
fun Waveform(level: Float, color: Color, modifier: Modifier = Modifier, bars: Int = 7, height: Dp = 22.dp) {
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        0f, (2 * Math.PI).toFloat(), infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "phase",
    )
    val l by animateFloatAsState(level, tween(80), label = "lvl")
    Canvas(modifier.size(width = (bars * 5).dp, height = height)) {
        val w = size.width / (bars * 2 - 1)
        for (i in 0 until bars) {
            val wobble = abs(sin(phase + i * 0.9f))
            val h = size.height * (0.18f + 0.82f * (0.25f + 0.75f * l) * (0.35f + 0.65f * wobble)).coerceIn(0.1f, 1f)
            drawRoundRect(
                color, Offset(i * 2 * w, (size.height - h) / 2), Size(w, h), CornerRadius(w / 2, w / 2),
            )
        }
    }
}
