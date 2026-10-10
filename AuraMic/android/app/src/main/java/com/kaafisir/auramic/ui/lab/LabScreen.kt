package com.kaafisir.auramic.ui.lab

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaafisir.auramic.R
import com.kaafisir.auramic.ui.components.AuraBubble
import com.kaafisir.auramic.ui.components.AuraCard
import com.kaafisir.auramic.ui.components.BubbleVisual
import com.kaafisir.auramic.ui.components.GradientButton
import com.kaafisir.auramic.ui.components.GradientText
import com.kaafisir.auramic.ui.components.IconBadge
import com.kaafisir.auramic.ui.theme.AuraCyan
import com.kaafisir.auramic.ui.theme.AuraGradient
import com.kaafisir.auramic.ui.theme.AuraMint
import com.kaafisir.auramic.ui.theme.AuraPink

@Composable
fun LabScreen(onBack: () -> Unit, vm: LabViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val level by vm.level.collectAsStateWithLifecycle()
    val playing by vm.playing.collectAsStateWithLifecycle()
    var needMic by remember { mutableStateOf(false) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { needMic = !it }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.common_back)) }
            GradientText(stringResource(R.string.home_lab_title), MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.lab_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))

            // Hold-to-record button
            Box(
                Modifier
                    .size(220.dp)
                    .pointerInput(state is LabState.Processing) {
                        if (state is LabState.Processing) return@pointerInput
                        awaitEachGesture {
                            awaitFirstDown()
                            if (!vm.hasMic()) { micLauncher.launch(Manifest.permission.RECORD_AUDIO); return@awaitEachGesture }
                            if (vm.start()) {
                                waitForUpOrCancellation()
                                vm.stop()
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                AuraBubble(
                    visual = when (state) {
                        LabState.Recording -> BubbleVisual.Recording
                        is LabState.Processing -> BubbleVisual.Processing
                        is LabState.Result -> BubbleVisual.Success
                        else -> BubbleVisual.Idle
                    },
                    level = level, bubbleSize = 96.dp, modifier = Modifier.fillMaxSize(), glow = true,
                )
            }
            Spacer(Modifier.height(8.dp))
            AnimatedContent(state, label = "labstatus") { s ->
                Text(
                    when (s) {
                        LabState.Recording -> stringResource(R.string.lab_recording)
                        is LabState.Processing -> stringResource(R.string.lab_processing) + " " + "${(s.progress * 100).toInt()}%"
                        LabState.TooShort -> stringResource(R.string.lab_too_short)
                        is LabState.Result -> stringResource(R.string.lab_done)
                        LabState.Idle -> if (needMic) stringResource(R.string.lab_mic_needed) else stringResource(R.string.lab_hold)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (s is LabState.Result) AuraMint else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(24.dp))

            (state as? LabState.Result)?.let { r ->
                Text(
                    stringResource(R.string.lab_info, r.processingMs / 1000f),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                TakeCard(stringResource(R.string.lab_original), Icons.Rounded.GraphicEq, AuraPink, playing == Take.Original) { vm.play(Take.Original) }
                Spacer(Modifier.height(12.dp))
                TakeCard(stringResource(R.string.lab_clean), Icons.Rounded.AutoAwesome, AuraCyan, playing == Take.Clean) { vm.play(Take.Clean) }
                Spacer(Modifier.height(20.dp))
                GradientButton(stringResource(R.string.lab_again), { vm.reset() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Refresh, brush = AuraGradient)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TakeCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, playing: Boolean, onToggle: () -> Unit) {
    AuraCard(onClick = onToggle) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(icon, tint)
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                stringResource(if (playing) R.string.lab_stop else R.string.lab_play),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
            )
            Icon(if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
