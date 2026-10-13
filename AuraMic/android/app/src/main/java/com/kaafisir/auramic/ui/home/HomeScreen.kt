package com.kaafisir.auramic.ui.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaafisir.auramic.R
import com.kaafisir.auramic.data.update.UpdateState
import com.kaafisir.auramic.ui.pro.ProViewModel
import com.kaafisir.auramic.ui.pro.UpdateBanner
import com.kaafisir.auramic.ui.components.AuraBubble
import com.kaafisir.auramic.ui.components.AuraCard
import com.kaafisir.auramic.ui.components.BubbleVisual
import com.kaafisir.auramic.ui.components.GradientButton
import com.kaafisir.auramic.ui.components.GradientText
import com.kaafisir.auramic.ui.components.IconBadge
import com.kaafisir.auramic.ui.components.SectionHeader
import com.kaafisir.auramic.ui.components.rememberStartActivity
import com.kaafisir.auramic.ui.theme.AuraCyan
import com.kaafisir.auramic.ui.theme.AuraGradient
import com.kaafisir.auramic.ui.theme.AuraGradientWarm
import com.kaafisir.auramic.ui.theme.AuraMint
import com.kaafisir.auramic.ui.theme.AuraPink
import com.kaafisir.auramic.ui.theme.AuraViolet

@Composable
fun HomeScreen(
    onSetup: () -> Unit,
    onLab: () -> Unit,
    onSettings: () -> Unit,
    onReplayTour: () -> Unit,
    onOpenPro: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
    proVm: ProViewModel = hiltViewModel(),
) {
    val pro by proVm.ui.collectAsStateWithLifecycle()
    val updateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // LocalContext is the language-wrapped context here, not the Activity the review sheet needs.
    val activity = androidx.activity.compose.LocalActivity.current
    val startActivity = rememberStartActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                vm.refresh(); vm.ensureRunning()
                activity?.let { vm.maybeAskForReview(it) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val setupNeeded = !ui.permissions.requiredGranted
    val statusTitle = when {
        setupNeeded -> R.string.home_status_setup
        ui.running -> R.string.home_status_on
        else -> R.string.home_status_off
    }
    val statusBody = when {
        setupNeeded -> R.string.home_status_setup_body
        ui.running -> R.string.home_status_on_body
        else -> R.string.home_status_off_body
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(AuraGradient), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.GraphicEq, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(10.dp))
            GradientText(stringResource(R.string.app_name), MaterialTheme.typography.titleLarge, Modifier.weight(1f))
            IconButton(onClick = onSettings) {
                Icon(Icons.Rounded.Settings, stringResource(R.string.set_title), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ---- hero ------------------------------------------------------------------------------
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.linearGradient(
                        if (ui.running && !setupNeeded) listOf(Color(0xFF3B2BA8), Color(0xFF0E7490)) else listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainer)
                    )
                )
                .padding(24.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AuraBubble(
                    visual = BubbleVisual.Idle, level = 0f, bubbleSize = 84.dp,
                    modifier = Modifier.size(170.dp).alpha(if (ui.running && !setupNeeded) 1f else 0.55f), glow = ui.running,
                )
                AnimatedContent(statusTitle, label = "status") { t ->
                    Text(
                        stringResource(t), style = MaterialTheme.typography.headlineSmall,
                        color = if (ui.running && !setupNeeded) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(statusBody), style = MaterialTheme.typography.bodyMedium,
                    color = if (ui.running && !setupNeeded) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                if (setupNeeded) {
                    GradientButton(stringResource(R.string.home_fix_permissions), onSetup, Modifier.fillMaxWidth(), brush = AuraGradientWarm)
                } else {
                    GradientButton(
                        stringResource(if (ui.running) R.string.home_turn_off else R.string.home_turn_on),
                        { vm.toggle(onSetup) }, Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.PowerSettingsNew,
                        brush = if (ui.running) Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x33FFFFFF))) else AuraGradient,
                    )
                    if (ui.running) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val launch = com.kaafisir.auramic.domain.model.ChatApp.entries.flatMap { it.packages }
                                    .firstNotNullOfOrNull { p -> runCatching { context.packageManager.getLaunchIntentForPackage(p) }.getOrNull() }
                                if (launch != null) startActivity(launch)
                                else Toast.makeText(context, R.string.home_whatsapp_missing, Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.home_open_whatsapp), color = Color.White) }
                    }
                }
            }
        }

        // ---- update + Pro --------------------------------------------------------------------------
        if (pro.update != UpdateState.NONE) {
            Spacer(Modifier.height(16.dp))
            UpdateBanner(pro.update, onUpdate = { proVm.startUpdate(updateLauncher) }, onRestart = proVm::installUpdate)
        }
        if (!pro.pro.pro) {
            Spacer(Modifier.height(16.dp))
            AuraCard(onClick = onOpenPro) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconBadge(Icons.Rounded.WorkspacePremium, com.kaafisir.auramic.ui.theme.AuraAmber)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.pro_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (pro.pro.trialLeft > 0) stringResource(R.string.q_trial_left, pro.pro.trialLeft) else stringResource(R.string.q_trial_over),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ---- stats -----------------------------------------------------------------------------
        Spacer(Modifier.height(16.dp))
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Modifier.weight(1f), ui.settings.notesCleaned.toString(), stringResource(R.string.home_stat_notes), AuraViolet)
            StatCard(Modifier.weight(1f), "%.1f".format(ui.settings.secondsPolished / 60f), stringResource(R.string.home_stat_minutes), AuraCyan)
        }

        // ---- how it works ----------------------------------------------------------------------
        SectionHeader(stringResource(R.string.home_how_title))
        AuraCard {
            listOf(R.string.home_how_1, R.string.home_how_2, R.string.home_how_3).forEachIndexed { i, res ->
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(AuraGradient), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text(stringResource(res), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // ---- lab & tour --------------------------------------------------------------------------
        Spacer(Modifier.height(14.dp))
        AuraCard(onClick = onLab) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconBadge(Icons.Rounded.Science, AuraPink)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_lab_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.home_lab_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        AuraCard(onClick = onReplayTour) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconBadge(Icons.Rounded.PlayCircle, AuraMint)
                Text(stringResource(R.string.home_guide_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String, tint: Color) {
    // Both cards always have the same height; the label shrinks to fit on one line instead of wrapping.
    AuraCard(modifier.fillMaxHeight()) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = tint, maxLines = 1)
        BasicText(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 14.sp, stepSize = 0.5.sp),
        )
    }
}
