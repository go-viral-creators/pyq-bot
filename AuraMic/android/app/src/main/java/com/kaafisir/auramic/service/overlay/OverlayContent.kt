package com.kaafisir.auramic.service.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaafisir.auramic.R
import com.kaafisir.auramic.ui.components.AuraBubble
import com.kaafisir.auramic.ui.components.BubbleVisual
import com.kaafisir.auramic.ui.components.Waveform
import com.kaafisir.auramic.ui.theme.AuraCyan
import com.kaafisir.auramic.ui.theme.AuraMint
import com.kaafisir.auramic.ui.theme.AuraPink
import com.kaafisir.auramic.ui.theme.AuraRed

fun OverlayState.toVisual(): BubbleVisual = when (this) {
    is OverlayState.Recording -> if (cancelArmed) BubbleVisual.CancelArmed else BubbleVisual.Recording
    OverlayState.Processing -> BubbleVisual.Processing
    OverlayState.Sending -> BubbleVisual.Sending
    is OverlayState.Done -> if (ok) BubbleVisual.Success else BubbleVisual.Failure
    else -> BubbleVisual.Idle
}

/** Content of the touchable bubble window. */
@Composable
fun BubbleWindowContent(state: OverlayState, level: Float, bubbleSize: Dp) {
    // Pop-in when the user opens WhatsApp.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val appear by animateFloatAsState(
        if (shown) 1f else 0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "appear",
    )
    AuraBubble(
        visual = state.toVisual(),
        level = level,
        bubbleSize = bubbleSize,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { scaleX = appear; scaleY = appear; alpha = appear.coerceIn(0f, 1f) },
    )
}

/** Content of the (non-touchable, full-width) status pill window; the pill centres on [anchorX]. */
@Composable
fun PillWindowContent(state: OverlayState, level: Float, anchorX: Int) {
    Box(Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = state !is OverlayState.Idle,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier.layout { measurable, constraints ->
                val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                val margin = 8.dp.roundToPx()
                layout(constraints.maxWidth, p.height) {
                    val x = (anchorX - p.width / 2).coerceIn(margin, (constraints.maxWidth - p.width - margin).coerceAtLeast(margin))
                    p.place(x, 0)
                }
            },
        ) {
            StatusPill(state, level)
        }
    }
}

@Composable
private fun StatusPill(state: OverlayState, level: Float) {
    Row(
        Modifier
            .background(Color(0xE6101018), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state) {
            is OverlayState.Recording -> {
                val armed = state.cancelArmed
                Box(Modifier.size(8.dp).background(if (armed) AuraRed else AuraPink, CircleShape))
                if (armed) {
                    PillText(stringResource(R.string.ov_release_cancel), AuraRed)
                } else {
                    PillText(formatTime(state.elapsedMs), Color.White, bold = true)
                    Waveform(level, AuraCyan, bars = 6, height = 18.dp)
                    PillText(stringResource(R.string.ov_slide_cancel), Color(0xFFB4B4C8))
                }
            }
            OverlayState.Processing -> PillText(stringResource(R.string.ov_processing), Color.White)
            OverlayState.Sending -> PillText(stringResource(R.string.ov_sending), Color.White)
            is OverlayState.Done -> {
                Icon(
                    if (state.ok) Icons.Rounded.Check else Icons.Rounded.ErrorOutline, null,
                    tint = if (state.ok) AuraMint else AuraRed, modifier = Modifier.size(16.dp),
                )
                PillText(stringResource(state.message), Color.White)
            }
            is OverlayState.Hint -> PillText(stringResource(state.message), Color.White)
            OverlayState.Idle -> Unit
        }
    }
}

@Composable
private fun PillText(text: String, color: Color, bold: Boolean = false) {
    Text(text, color = color, fontSize = 13.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
}

fun formatTime(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
