package com.kaafisir.auramic.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kaafisir.auramic.R
import com.kaafisir.auramic.ui.theme.AuraCyan
import com.kaafisir.auramic.ui.theme.AuraIndigo
import com.kaafisir.auramic.ui.theme.AuraMint
import com.kaafisir.auramic.ui.theme.AuraPink
import com.kaafisir.auramic.ui.theme.AuraRed
import com.kaafisir.auramic.ui.theme.AuraViolet

enum class BubbleVisual { Idle, Recording, CancelArmed, Processing, Sending, Success, Failure }

/**
 * The floating mic. Drawn into whatever space the parent gives it (it is centred), with the disc
 * itself being [bubbleSize]; extra room is used for the pulsing rings while recording. Shared by
 * the real overlay window and the interactive tutorial.
 */
@Composable
fun AuraBubble(
    visual: BubbleVisual,
    level: Float,
    bubbleSize: Dp,
    modifier: Modifier = Modifier,
    glow: Boolean = false,
    /** On top of WhatsApp's own button: a flat disc exactly [bubbleSize] wide while idle (no breathing, no glow). */
    flat: Boolean = false,
) {
    val infinite = rememberInfiniteTransition(label = "bubble")
    val breathe by infinite.animateFloat(
        1f, 1.045f,
        infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe",
    )
    val ring by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "ring")
    val spin by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "spin")
    val animLevel by animateFloatAsState(level, tween(90), label = "level")
    val scale by animateFloatAsState(
        when (visual) {
            BubbleVisual.Recording -> 1.14f
            BubbleVisual.CancelArmed -> 0.9f
            else -> 1f
        },
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "scale",
    )

    val colors = when (visual) {
        BubbleVisual.Idle -> listOf(AuraViolet, AuraCyan)
        BubbleVisual.Recording -> listOf(AuraViolet, AuraPink)
        BubbleVisual.CancelArmed -> listOf(Color(0xFF55556B), AuraRed)
        BubbleVisual.Processing -> listOf(AuraIndigo, AuraViolet)
        BubbleVisual.Sending -> listOf(AuraCyan, AuraViolet)
        BubbleVisual.Success -> listOf(AuraMint, Color(0xFF10B981))
        BubbleVisual.Failure -> listOf(AuraRed, Color(0xFFB91C4A))
    }
    val ringColor = if (visual == BubbleVisual.CancelArmed) AuraRed else AuraPink

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            val base = bubbleSize.toPx() / 2
            val r = base * scale * (if (visual == BubbleVisual.Idle && !flat) breathe else 1f)

            // soft outer glow
            if (!(flat && visual == BubbleVisual.Idle)) {
            val glowAlpha = if (glow || visual == BubbleVisual.Idle) 0.22f else 0.32f
            drawCircle(
                Brush.radialGradient(listOf(colors[0].copy(alpha = glowAlpha), Color.Transparent), c, r * 1.4f),
                radius = r * 1.4f, center = c,
            )
            }
            // pulsing rings while recording, driven by the live mic level
            if (visual == BubbleVisual.Recording || visual == BubbleVisual.CancelArmed) {
                for (phase in listOf(0f, 0.5f)) {
                    val p = (ring + phase) % 1f
                    val rr = r * (1.08f + p * (0.55f + animLevel * 0.35f))
                    drawCircle(ringColor.copy(alpha = (1f - p) * 0.38f), radius = rr, center = c, style = Stroke(width = base * 0.07f))
                }
            }
            // the disc
            drawCircle(
                Brush.linearGradient(colors, Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r)),
                radius = r, center = c,
            )
            // glassy highlight
            drawCircle(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent), Offset(c.x - r * 0.35f, c.y - r * 0.4f), r * 0.9f),
                radius = r, center = c,
            )
            // processing / sending: orbiting arc
            if (visual == BubbleVisual.Processing || visual == BubbleVisual.Sending) {
                val ro = r * 1.2f
                drawArc(
                    Brush.sweepGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f)), c),
                    startAngle = spin, sweepAngle = 110f, useCenter = false,
                    topLeft = Offset(c.x - ro, c.y - ro), size = Size(ro * 2, ro * 2),
                    style = Stroke(width = base * 0.09f, cap = StrokeCap.Round),
                )
            }
        }
        Crossfade(visual, Modifier.size(bubbleSize), label = "icon") { v ->
            Box(Modifier.size(bubbleSize), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = v.icon(),
                    contentDescription = if (v == BubbleVisual.Idle) stringResource(R.string.cd_mic) else null,
                    tint = Color.White,
                    modifier = Modifier.size(bubbleSize * 0.46f),
                )
            }
        }
    }
}

private fun BubbleVisual.icon(): ImageVector = when (this) {
    BubbleVisual.Idle, BubbleVisual.Recording -> Icons.Rounded.Mic
    BubbleVisual.CancelArmed -> Icons.Rounded.Close
    BubbleVisual.Processing -> Icons.Rounded.AutoAwesome
    BubbleVisual.Sending -> Icons.Rounded.Send
    BubbleVisual.Success -> Icons.Rounded.Check
    BubbleVisual.Failure -> Icons.Rounded.ErrorOutline
}
