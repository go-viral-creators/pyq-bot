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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kaafisir.auramic.data.notes.NoteArchive
import com.kaafisir.auramic.data.notes.SavedNote
import com.kaafisir.auramic.ui.components.SectionHeader
import com.kaafisir.auramic.ui.theme.AuraRed
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
        val notes by vm.notes.collectAsStateWithLifecycle()
        val saving by vm.savingEnabled.collectAsStateWithLifecycle()
        val now by vm.nowPlaying.collectAsStateWithLifecycle()
        val failed by vm.playFailed.collectAsStateWithLifecycle()
        var confirmDelete by remember { mutableStateOf<SavedNote?>(null) }
        var confirmDeleteAll by remember { mutableStateOf(false) }

        LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        LabState.Failed -> BubbleVisual.Failure
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
                        LabState.Failed -> stringResource(R.string.lab_failed)
                        is LabState.Result -> stringResource(R.string.lab_done)
                        LabState.Idle -> if (needMic) stringResource(R.string.lab_mic_needed) else stringResource(R.string.lab_hold)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = when (s) { is LabState.Result -> AuraMint; LabState.Failed -> AuraRed; else -> MaterialTheme.colorScheme.onSurface },
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
            }
            }

            // ---- saved voice notes ---------------------------------------------------------------------------------
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader(stringResource(R.string.lab_notes_title), Modifier.weight(1f))
                    val loaded = notes as? NoteArchive.State.Loaded
                    if (loaded != null && loaded.notes.isNotEmpty()) {
                        IconButton(onClick = { confirmDeleteAll = true }, modifier = Modifier.padding(top = 14.dp)) {
                            Icon(Icons.Rounded.DeleteSweep, stringResource(R.string.lab_delete_all), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            when (val n = notes) {
                NoteArchive.State.Loading -> item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(10.dp))
                        Text(stringResource(R.string.lab_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is NoteArchive.State.Loaded -> {
                    item {
                        val ctx = LocalContext.current
                        Column(Modifier.fillMaxWidth()) {
                            if (!saving) InfoText(stringResource(R.string.lab_notes_off))
                            if (n.notes.isEmpty()) {
                                if (saving) InfoText(stringResource(R.string.lab_notes_empty))
                            } else {
                                InfoText(stringResource(R.string.lab_storage, n.notes.size, android.text.format.Formatter.formatShortFileSize(ctx, n.totalBytes)) +
                                    "  ·  " + stringResource(R.string.lab_compare_hint))
                            }
                        }
                    }
                    items(n.notes, key = { it.id }) { note ->
                        SavedNoteCard(
                            note = note, now = now?.takeIf { it.noteId == note.id }, failed = failed,
                            onToggle = { take -> vm.togglePlay(note, take) },
                            onSeek = { take, f -> vm.seek(note, take, f) },
                            onDelete = { confirmDelete = note },
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }

        confirmDelete?.let { note ->
            AlertDialog(
                onDismissRequest = { confirmDelete = null },
                title = { Text(stringResource(R.string.lab_delete_title)) },
                text = { Text(stringResource(R.string.lab_delete_body)) },
                confirmButton = { TextButton(onClick = { vm.delete(note); confirmDelete = null }) { Text(stringResource(R.string.lab_delete), color = AuraRed) } },
                dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
            )
        }
        if (confirmDeleteAll) {
            AlertDialog(
                onDismissRequest = { confirmDeleteAll = false },
                title = { Text(stringResource(R.string.lab_delete_all_title)) },
                text = { Text(stringResource(R.string.lab_delete_body)) },
                confirmButton = { TextButton(onClick = { vm.deleteAll(); confirmDeleteAll = false }) { Text(stringResource(R.string.lab_delete_all), color = AuraRed) } },
                dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text(stringResource(R.string.common_cancel)) } },
            )
        }
    }
}

@Composable
private fun InfoText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
}

/** One saved voice note: its time, and the original / enhanced takes as two clearly labelled players. */
@Composable
internal fun SavedNoteCard(
    note: SavedNote, now: NowPlaying?, failed: String?,
    onToggle: (Take) -> Unit, onSeek: (Take, Float) -> Unit, onDelete: () -> Unit,
) {
    val ctx = LocalContext.current
    val whenText = remember(note.createdAt) {
        android.text.format.DateUtils.formatDateTime(ctx, note.createdAt,
            android.text.format.DateUtils.FORMAT_SHOW_DATE or android.text.format.DateUtils.FORMAT_SHOW_TIME or android.text.format.DateUtils.FORMAT_ABBREV_MONTH)
    }
    AuraCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(whenText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, stringResource(R.string.lab_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.height(6.dp))
        TakeRow(
            label = stringResource(R.string.lab_original_voice), icon = Icons.Rounded.GraphicEq, tint = AuraPink,
            available = note.originalFile != null, peaks = note.originalPeaks, totalMs = note.originalMs,
            now = now?.takeIf { it.take == Take.Original }, failed = failed == "${note.id}/${Take.Original.name}",
            onToggle = { onToggle(Take.Original) }, onSeek = { onSeek(Take.Original, it) },
        )
        Spacer(Modifier.height(10.dp))
        TakeRow(
            label = stringResource(R.string.lab_enhanced_voice), icon = Icons.Rounded.AutoAwesome, tint = AuraCyan,
            available = note.enhancedFile != null, peaks = note.enhancedPeaks, totalMs = note.enhancedMs,
            now = now?.takeIf { it.take == Take.Clean }, failed = failed == "${note.id}/${Take.Clean.name}",
            onToggle = { onToggle(Take.Clean) }, onSeek = { onSeek(Take.Clean, it) },
        )
    }
}

@Composable
private fun TakeRow(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color,
    available: Boolean, peaks: FloatArray, totalMs: Long, now: NowPlaying?, failed: Boolean,
    onToggle: () -> Unit, onSeek: (Float) -> Unit,
) {
    val playing = now?.playing == true
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        IconBadge(icon, tint, size = 36.dp)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = tint, modifier = Modifier.weight(1f))
                val dur = now?.durationMs?.takeIf { it > 0 } ?: totalMs
                Text(
                    if (!available) stringResource(R.string.lab_not_saved)
                    else if (now != null) "${formatMs(now.positionMs)} / ${formatMs(dur)}" else formatMs(dur),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            PeakBar(peaks, now?.fraction ?: 0f, tint, enabled = available, onSeek = onSeek)
            if (failed) Text(stringResource(R.string.lab_play_failed), style = MaterialTheme.typography.labelMedium, color = AuraRed)
        }
        IconButton(onClick = onToggle, enabled = available) {
            Icon(
                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                stringResource(if (playing) R.string.lab_pause else R.string.lab_play) + " " + label,
                tint = if (available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** Static waveform (stored peaks) with the played part highlighted; tap or drag to seek. */
@Composable
private fun PeakBar(peaks: FloatArray, progress: Float, tint: Color, enabled: Boolean, onSeek: (Float) -> Unit) {
    val dim = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures { change, _ -> onSeek((change.position.x / size.width).coerceIn(0f, 1f)) }
            },
    ) {
        val bars = if (peaks.isEmpty()) 60 else peaks.size
        val w = size.width / bars
        for (i in 0 until bars) {
            val p = if (peaks.isEmpty()) 0.15f else (0.12f + 0.88f * kotlin.math.sqrt(peaks[i]))
            val h = size.height * p.coerceIn(0.08f, 1f)
            val played = (i + 0.5f) / bars <= progress
            drawRoundRect(
                if (played && enabled) tint else dim,
                Offset(i * w + w * 0.15f, (size.height - h) / 2), Size(w * 0.7f, h), CornerRadius(w * 0.35f, w * 0.35f),
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
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
