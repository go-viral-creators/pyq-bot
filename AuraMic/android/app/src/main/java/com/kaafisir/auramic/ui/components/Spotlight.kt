package com.kaafisir.auramic.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaafisir.auramic.R
import com.kaafisir.auramic.ui.theme.AuraCyan
import com.kaafisir.auramic.ui.theme.AuraViolet
import kotlin.math.max

/**
 * "Showcase" scrim: dims everything except [target] (animated between steps) and pulses a
 * ring around the hole. It does not consume touches, so the highlighted widget stays usable.
 */
@Composable
fun SpotlightScrim(
    target: Rect?,
    modifier: Modifier = Modifier,
    circle: Boolean = false,
    cornerRadius: Float = 48f,
    padding: Float = 14f,
) {
    val anim by animateRectAsState(target ?: Rect.Zero, tween(450, easing = FastOutSlowInEasing), label = "spot")
    val pulse by rememberInfiniteTransition(label = "spotPulse").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1300), RepeatMode.Restart), label = "pulse",
    )
    Canvas(modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        drawRect(Color(0xD0060610))
        if (target == null) return@Canvas
        val r = anim
        if (circle) {
            val rad = max(r.width, r.height) / 2f + padding
            drawCircle(Color.Black, rad, r.center, blendMode = BlendMode.Clear)
            drawCircle(AuraCyan.copy(alpha = (1f - pulse) * 0.7f), rad + pulse * 34f, r.center, style = Stroke(3.dp.toPx()))
            drawCircle(AuraViolet.copy(alpha = 0.9f), rad, r.center, style = Stroke(2.dp.toPx()))
        } else {
            val tl = Offset(r.left - padding, r.top - padding)
            val sz = Size(r.width + padding * 2, r.height + padding * 2)
            drawRoundRect(Color.Black, tl, sz, CornerRadius(cornerRadius), blendMode = BlendMode.Clear)
            val g = pulse * 24f
            drawRoundRect(
                AuraCyan.copy(alpha = (1f - pulse) * 0.7f), Offset(tl.x - g, tl.y - g), Size(sz.width + g * 2, sz.height + g * 2),
                CornerRadius(cornerRadius + g), style = Stroke(3.dp.toPx()),
            )
            drawRoundRect(AuraViolet.copy(alpha = 0.9f), tl, sz, CornerRadius(cornerRadius), style = Stroke(2.dp.toPx()))
        }
    }
}

/** Tooltip card shown next to the highlighted element. */
@Composable
fun TourTooltip(
    step: Int,
    total: Int,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEmphasis: Boolean = true,
) {
    Surface(
        modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 16.dp,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.tour_step_of, step, total),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
            )
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSkip) { Text(stringResource(R.string.common_skip)) }
                if (primaryEmphasis) {
                    GradientButton(primaryLabel, onPrimary, Modifier)
                } else {
                    TextButton(onClick = onPrimary) { Text(primaryLabel) }
                }
            }
        }
    }
}
