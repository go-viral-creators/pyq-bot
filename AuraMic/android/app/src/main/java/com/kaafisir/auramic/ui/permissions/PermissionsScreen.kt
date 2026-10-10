package com.kaafisir.auramic.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaafisir.auramic.R
import com.kaafisir.auramic.domain.model.PermissionState
import com.kaafisir.auramic.ui.components.AuraCard
import com.kaafisir.auramic.ui.components.GradientButton
import com.kaafisir.auramic.ui.components.GradientText
import com.kaafisir.auramic.ui.components.IconBadge
import com.kaafisir.auramic.ui.components.rememberStartActivity
import com.kaafisir.auramic.ui.components.SpotlightScrim
import com.kaafisir.auramic.ui.components.TourTooltip
import com.kaafisir.auramic.ui.theme.AuraAmber
import com.kaafisir.auramic.ui.theme.AuraCyan
import com.kaafisir.auramic.ui.theme.AuraGradient
import com.kaafisir.auramic.ui.theme.AuraMint
import com.kaafisir.auramic.ui.theme.AuraPink
import com.kaafisir.auramic.ui.theme.AuraViolet

private class PermUi(
    val perm: Perm, val icon: ImageVector, val tint: Color,
    @StringRes val title: Int, @StringRes val body: Int, val required: Boolean,
)

private val ALL = listOf(
    PermUi(Perm.Overlay, Icons.Rounded.Layers, AuraViolet, R.string.perm_overlay_title, R.string.perm_overlay_body, true),
    PermUi(Perm.Microphone, Icons.Rounded.Mic, AuraPink, R.string.perm_mic_title, R.string.perm_mic_body, true),
    PermUi(Perm.Usage, Icons.Rounded.QueryStats, AuraCyan, R.string.perm_usage_title, R.string.perm_usage_body, true),
    PermUi(Perm.Accessibility, Icons.Rounded.TouchApp, AuraMint, R.string.perm_access_title, R.string.perm_access_body, true),
    PermUi(Perm.Notifications, Icons.Rounded.Notifications, AuraAmber, R.string.perm_notif_title, R.string.perm_notif_body, true),
    PermUi(Perm.Battery, Icons.Rounded.BatteryChargingFull, Color(0xFF8BC34A), R.string.perm_battery_title, R.string.perm_battery_body, false),
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun PermissionsScreen(
    showcase: Boolean,
    onDone: () -> Unit,
    vm: PermissionsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val startActivity = rememberStartActivity()
    val state by vm.state.collectAsStateWithLifecycle()

    // Re-read permissions whenever we come back from a settings screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) vm.refresh() }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    fun open(p: Perm) {
        val i = vm.settingsIntent(p) ?: return
        if (!startActivity(i)) startActivity(vm.appDetailsIntent())
    }

    var micAsked by rememberSaveable { mutableStateOf(false) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refresh() }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.refresh() }
    var showDisclosure by rememberSaveable { mutableStateOf(false) }
    var triedAccessibility by rememberSaveable { mutableStateOf(false) }

    fun onAllow(p: Perm) {
        when (p) {
            Perm.Microphone -> {
                val blocked = micAsked && activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
                if (blocked) startActivity(vm.appDetailsIntent())
                else { micAsked = true; micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            }
            Perm.Notifications -> if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            Perm.Accessibility -> showDisclosure = true // Google Play prominent-disclosure requirement
            else -> open(p)
        }
    }

    // ---- showcase (first run only) -----------------------------------------------------------
    var tip by rememberSaveable { mutableIntStateOf(if (showcase) 0 else -1) }
    val bounds = remember { mutableStateMapOf<Perm, Rect>() }
    var root by remember { mutableStateOf(IntSize.Zero) }
    val scroll = rememberScrollState()

    val granted = ALL.filter { it.required }.count { vm.granted(it.perm, state) }
    val progress by animateFloatAsState(granted / PermissionState.REQUIRED_TOTAL.toFloat(), label = "progress")

    Box(Modifier.fillMaxSize().onSizeChanged { root = it }) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
        ) {
            Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(24.dp))
                GradientText(stringResource(R.string.perm_title), MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.perm_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(Modifier.fillMaxWidth(progress).height(8.dp).clip(CircleShape).background(AuraGradient))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.perm_progress, granted, PermissionState.REQUIRED_TOTAL),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                ALL.forEach { ui ->
                    val ok = vm.granted(ui.perm, state)
                    PermissionCard(
                        ui, ok, onAllow = { onAllow(ui.perm); if (ui.perm == Perm.Accessibility) triedAccessibility = true },
                        modifier = Modifier.onGloballyPositioned { bounds[ui.perm] = it.boundsInRoot() },
                    )
                    if (ui.perm == Perm.Accessibility) {
                        AnimatedVisibility(!ok && triedAccessibility && Build.VERSION.SDK_INT >= 33) {
                            RestrictedHelp(onOpenInfo = { startActivity(vm.appDetailsIntent()) })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(12.dp))
            }

            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GradientButton(stringResource(R.string.common_continue), onDone, Modifier.fillMaxWidth(), enabled = state.requiredGranted)
                TextButton(onClick = onDone) { Text(stringResource(R.string.perm_later), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }

        if (tip >= 0 && root.height > 0) {
            val perm = if (tip == 0) Perm.Overlay else Perm.Accessibility
            val rect = bounds[perm]
            LaunchedEffect(tip, rect == null) {
                if (tip == 1 && rect != null && rect.bottom > root.height * 0.7f) scroll.animateScrollTo(scroll.value + (rect.top - root.height * 0.25f).toInt())
            }
            SpotlightScrim(rect, circle = false, cornerRadius = 64f, padding = 8f)
            val atBottom = (rect?.center?.y ?: 0f) < root.height * 0.5f
            Box(
                Modifier.fillMaxSize().navigationBarsPadding().statusBarsPadding().padding(vertical = 24.dp),
                contentAlignment = if (atBottom) Alignment.BottomCenter else Alignment.TopCenter,
            ) {
                TourTooltip(
                    step = tip + 1, total = 2,
                    title = stringResource(if (tip == 0) R.string.tour_p1_title else R.string.tour_p2_title),
                    body = stringResource(if (tip == 0) R.string.tour_p1_body else R.string.tour_p2_body),
                    primaryLabel = stringResource(if (tip == 0) R.string.common_next else R.string.common_done),
                    onPrimary = { tip = if (tip == 0) 1 else -1 },
                    onSkip = { tip = -1 },
                )
            }
        }
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            icon = { Icon(Icons.Rounded.TouchApp, null, tint = AuraMint) },
            title = { Text(stringResource(R.string.disclosure_title)) },
            text = { Text(stringResource(R.string.disclosure_body), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { showDisclosure = false; open(Perm.Accessibility) }) { Text(stringResource(R.string.disclosure_agree)) }
            },
            dismissButton = { TextButton(onClick = { showDisclosure = false }) { Text(stringResource(R.string.disclosure_decline)) } },
        )
    }
}

@Composable
private fun PermissionCard(ui: PermUi, granted: Boolean, onAllow: () -> Unit, modifier: Modifier = Modifier) {
    val border by animateColorAsState(if (granted) AuraMint.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant, label = "border")
    AuraCard(modifier, borderBrush = androidx.compose.ui.graphics.SolidColor(border)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(ui.icon, ui.tint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(ui.title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(if (ui.required) R.string.perm_required else R.string.perm_optional),
                        fontSize = 10.sp, color = if (ui.required) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Text(stringResource(ui.body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (granted) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(AuraMint), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, stringResource(R.string.perm_granted), tint = Color.White, modifier = Modifier.size(18.dp))
                }
            } else {
                GradientButton(stringResource(R.string.perm_allow), onAllow, Modifier, brush = AuraGradient)
            }
        }
    }
}

@Composable
private fun RestrictedHelp(onOpenInfo: () -> Unit) {
    AuraCard(Modifier.padding(top = 10.dp), borderBrush = androidx.compose.ui.graphics.SolidColor(AuraAmber.copy(alpha = 0.6f))) {
        Text(stringResource(R.string.perm_restricted_title), style = MaterialTheme.typography.titleMedium, color = AuraAmber)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.perm_restricted_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onOpenInfo) { Text(stringResource(R.string.perm_open_app_info)) }
    }
}
