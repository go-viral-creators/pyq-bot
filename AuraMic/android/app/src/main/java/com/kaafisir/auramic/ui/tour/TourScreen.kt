package com.kaafisir.auramic.ui.tour

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaafisir.auramic.R
import com.kaafisir.auramic.service.overlay.BubbleGesture
import com.kaafisir.auramic.ui.components.AuraBubble
import com.kaafisir.auramic.ui.components.BubbleVisual
import com.kaafisir.auramic.ui.components.SpotlightScrim
import com.kaafisir.auramic.ui.components.TourTooltip
import com.kaafisir.auramic.ui.components.Waveform
import com.kaafisir.auramic.ui.theme.AuraCyan
import com.kaafisir.auramic.ui.theme.AuraGradient
import com.kaafisir.auramic.ui.theme.AuraMint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class Step { Intro, Hold, Release, Cancel, Drag, Perms }

private const val TOTAL_STEPS = 6

/**
 * Interactive "showcase" tutorial. A mock chat hosts the *real* floating-mic composable with the
 * *same* gesture state machine as the overlay, so users learn hold / release / slide-to-cancel /
 * drag by actually doing them, with a spotlight and tooltip guiding each step.
 */
@Composable
fun TourScreen(onFinished: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(Step.Intro) }
    var visual by remember { mutableStateOf(BubbleVisual.Idle) }
    var level by remember { mutableFloatStateOf(0f) }
    var sentCount by remember { mutableIntStateOf(0) }
    var banner by remember { mutableStateOf<String?>(null) }
    var holdMs by remember { mutableIntStateOf(0) }
    var container by remember { mutableStateOf(IntSize.Zero) }
    var cx by remember { mutableFloatStateOf(-1f) }
    var cy by remember { mutableFloatStateOf(-1f) }
    var disc by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val bubbleDp = 72.dp
    val bubblePx = with(density) { bubbleDp.toPx() }
    val margin = with(density) { 12.dp.toPx() }

    val sLabelHold = stringResource(R.string.tour_demo_hold_prompt)
    val sLabelRelease = stringResource(R.string.tour_demo_release_prompt)
    val sLabelClean = stringResource(R.string.tour_demo_cleaning)
    val sLabelSent = stringResource(R.string.tour_demo_sent)

    LaunchedEffect(container) {
        if (container.width > 0 && cx < 0f) {
            cx = container.width - margin - bubblePx / 2
            cy = container.height * 0.6f
        }
    }

    // Drives the fake mic level / timer while the bubble is held.
    var holdJob by remember { mutableStateOf<Job?>(null) }

    val gesture = remember {
        BubbleGesture(
            scheduler = { d, action ->
                val job = scope.launch { delay(d); action() }
                BubbleGesture.Cancelable { job.cancel() }
            },
            listener = object : BubbleGesture.Listener {
                override fun onHoldStart() {
                    visual = BubbleVisual.Recording
                    banner = sLabelHold
                    holdMs = 0
                    holdJob?.cancel()
                    holdJob = scope.launch {
                        var t = 0
                        while (isActive) {
                            level = (0.35f + 0.4f * (sin(t / 3.1f) * 0.5f + 0.5f) + 0.2f * (sin(t / 1.3f) * 0.5f + 0.5f)).coerceIn(0f, 1f)
                            holdMs = t * 40
                            if (holdMs >= 1000 && step == Step.Hold) { step = Step.Release }
                            if (holdMs >= 1000 && step == Step.Release) banner = sLabelRelease
                            t++
                            delay(40)
                        }
                    }
                }

                override fun onCancelArmed(armed: Boolean) {
                    visual = if (armed) BubbleVisual.CancelArmed else BubbleVisual.Recording
                }

                override fun onRelease(cancelled: Boolean) {
                    holdJob?.cancel(); level = 0f
                    val longEnough = holdMs >= 700
                    when {
                        cancelled -> {
                            visual = BubbleVisual.Idle; banner = null
                            if (step == Step.Cancel) scope.launch { delay(500); step = Step.Drag }
                        }
                        !longEnough -> { visual = BubbleVisual.Idle; banner = null }
                        else -> scope.launch {
                            visual = BubbleVisual.Processing; banner = sLabelClean; delay(1200)
                            visual = BubbleVisual.Sending; delay(700)
                            visual = BubbleVisual.Success; banner = sLabelSent; sentCount++
                            delay(1400)
                            visual = BubbleVisual.Idle; banner = null
                            if (step == Step.Release || step == Step.Hold) step = Step.Cancel
                        }
                    }
                }

                override fun onDrag(dx: Float, dy: Float) {
                    cx = (cx + dx).coerceIn(margin + bubblePx / 2, container.width - margin - bubblePx / 2)
                    cy = (cy + dy).coerceIn(bubblePx, container.height - bubblePx * 1.5f)
                }

                override fun onDragEnd() {
                    val target = if (cx < container.width / 2f) margin + bubblePx / 2 else container.width - margin - bubblePx / 2
                    scope.launch {
                        animate(cx, target) { v, _ -> cx = v }
                        if (step == Step.Drag) { delay(400); step = Step.Perms }
                    }
                }

                override fun onTap() {}
            },
            slopPx = with(density) { 10.dp.toPx() },
            cancelDistancePx = with(density) { 64.dp.toPx() },
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onSizeChanged { container = it },
    ) {
        MockChat(sentCount)

        val showSpot = step != Step.Perms && cx >= 0f
        val target = if (showSpot) Rect(Offset(cx - bubblePx / 2, cy - bubblePx / 2), Size(bubblePx, bubblePx)) else null
        SpotlightScrim(target, circle = true)

        if (cx >= 0f) {
            // Status pill above/below the bubble
            banner?.let { text ->
                val above = cy > container.height * 0.3f
                Row(
                    Modifier
                        .offset { IntOffset((cx - with(density) { 110.dp.toPx() }).roundToInt().coerceIn(0, (container.width - with(density) { 220.dp.toPx() }).roundToInt().coerceAtLeast(0)), (if (above) cy - bubblePx * 1.25f else cy + bubblePx * 0.75f).roundToInt()) }
                        .width(220.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xE6101018))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (visual == BubbleVisual.Recording) {
                        Waveform(level, AuraCyan, bars = 6, height = 16.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(text, color = Color.White, fontSize = 13.sp, maxLines = 1)
                }
            }

            val windowDp = bubbleDp * 2
            Box(
                Modifier
                    .offset { IntOffset((cx - bubblePx).roundToInt(), (cy - bubblePx).roundToInt()) }
                    .size(windowDp),
                contentAlignment = Alignment.Center,
            ) {
                AuraBubble(visual, level, bubbleDp, Modifier.fillMaxSize(), glow = step != Step.Perms)
                // The touch target is only the disc, like the real overlay.
                Box(
                    Modifier
                        .size(bubbleDp)
                        .onGloballyPositioned { disc = it }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                fun root(o: Offset): Offset = (disc?.positionInRoot() ?: Offset.Zero) + o
                                val p0 = root(down.position)
                                gesture.down(p0.x, p0.y)
                                down.consume()
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!c.pressed) { c.consume(); break }
                                    val p = root(c.position)
                                    gesture.move(p.x, p.y)
                                    c.consume()
                                }
                                gesture.up()
                            }
                        },
                )
            }
        }

        // Tooltip
        val tooltipAtTop = cy > container.height * 0.5f || step == Step.Perms
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 16.dp),
            contentAlignment = if (tooltipAtTop) Alignment.TopCenter else Alignment.BottomCenter,
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn() + slideInVertically { if (tooltipAtTop) -it / 4 else it / 4 }) togetherWith
                        (fadeOut() + slideOutVertically { if (tooltipAtTop) -it / 4 else it / 4 })
                },
                label = "tooltip",
            ) { s ->
                val (title, body) = copyFor(s)
                val holding = visual == BubbleVisual.Recording
                TourTooltip(
                    step = s.ordinal + 1,
                    total = TOTAL_STEPS,
                    title = title,
                    body = if (s == Step.Hold && holding) stringResource(R.string.tour_t2_holding) else body,
                    primaryLabel = if (s == Step.Perms) stringResource(R.string.common_continue) else stringResource(R.string.common_next),
                    onPrimary = { step = nextOf(s) ?: run { onFinished(); s } },
                    onSkip = onFinished,
                    primaryEmphasis = s == Step.Intro || s == Step.Perms,
                    modifier = Modifier.animateContentSize(),
                )
            }
        }
    }
}

private fun nextOf(s: Step): Step? = when (s) {
    Step.Intro -> Step.Hold
    Step.Hold -> Step.Release
    Step.Release -> Step.Cancel
    Step.Cancel -> Step.Drag
    Step.Drag -> Step.Perms
    Step.Perms -> null
}

@Composable
private fun copyFor(s: Step): Pair<String, String> = when (s) {
    Step.Intro -> stringResource(R.string.tour_t1_title) to stringResource(R.string.tour_t1_body)
    Step.Hold -> stringResource(R.string.tour_t2_title) to stringResource(R.string.tour_t2_body)
    Step.Release -> stringResource(R.string.tour_t3_title) to stringResource(R.string.tour_t3_body)
    Step.Cancel -> stringResource(R.string.tour_t4_title) to stringResource(R.string.tour_t4_body)
    Step.Drag -> stringResource(R.string.tour_t5_title) to stringResource(R.string.tour_t5_body)
    Step.Perms -> stringResource(R.string.tour_t6_title) to stringResource(R.string.tour_t6_body)
}

// ------------------------------------------------------------------------------------------
// Decorative mock conversation (deliberately not WhatsApp-branded)
// ------------------------------------------------------------------------------------------

@Composable
private fun MockChat(sentVoiceNotes: Int) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(cs.background)) {
        Row(
            Modifier.fillMaxWidth().background(cs.surfaceContainerHigh).statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = cs.onSurface)
            Box(Modifier.size(38.dp).clip(CircleShape).background(AuraGradient), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.tour_chat_name).take(1), color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.tour_chat_name), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.tour_chat_status), style = MaterialTheme.typography.labelMedium, color = AuraMint)
            }
            Icon(Icons.Rounded.Call, null, tint = cs.onSurfaceVariant)
            Icon(Icons.Rounded.MoreVert, null, tint = cs.onSurfaceVariant)
        }
        Column(Modifier.weight(1f).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Bubble(stringResource(R.string.tour_msg_1), incoming = true)
            Bubble(stringResource(R.string.tour_msg_2), incoming = false)
            repeat(sentVoiceNotes.coerceAtMost(3)) { VoiceBubble() }
        }
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(50)).background(cs.surfaceContainerHigh).padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.height(10.dp).width(110.dp).clip(CircleShape).background(cs.outline.copy(alpha = 0.5f)))
                Icon(Icons.Rounded.AddCircleOutline, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(56.dp))
        }
    }
}

@Composable
private fun Bubble(text: String, incoming: Boolean) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (incoming) Arrangement.Start else Arrangement.End) {
        Box(
            Modifier
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (incoming) 4.dp else 18.dp, bottomEnd = if (incoming) 18.dp else 4.dp))
                .background(if (incoming) cs.surfaceContainerHigh else cs.primaryContainer)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) { Text(text, style = MaterialTheme.typography.bodyMedium, color = if (incoming) cs.onSurface else cs.onPrimaryContainer) }
    }
}

@Composable
private fun VoiceBubble() {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Row(
            Modifier.clip(RoundedCornerShape(18.dp)).background(cs.primaryContainer).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = cs.onPrimaryContainer)
            val heights = listOf(8, 14, 22, 12, 26, 18, 10, 24, 16, 9, 20, 13, 7)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                heights.forEach { Box(Modifier.width(3.dp).height(it.dp).clip(CircleShape).background(cs.primary)) }
            }
            Text("0:05", style = MaterialTheme.typography.labelMedium, color = cs.onPrimaryContainer)
        }
    }
}
