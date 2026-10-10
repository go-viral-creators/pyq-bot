package com.kaafisir.auramic.ui.settings

import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaafisir.auramic.BuildConfig
import com.kaafisir.auramic.R
import com.kaafisir.auramic.core.Links
import com.kaafisir.auramic.domain.model.AppLanguage
import com.kaafisir.auramic.domain.model.ChatApp
import com.kaafisir.auramic.domain.model.EnhanceEngine
import com.kaafisir.auramic.domain.model.OutputFormat
import com.kaafisir.auramic.domain.model.SilenceCut
import com.kaafisir.auramic.domain.model.ThemeMode
import com.kaafisir.auramic.domain.model.VoicePreset
import com.kaafisir.auramic.ui.components.AuraCard
import com.kaafisir.auramic.ui.components.GradientText
import com.kaafisir.auramic.ui.components.SectionHeader
import com.kaafisir.auramic.ui.components.rememberStartActivity
import com.kaafisir.auramic.ui.language.LanguageCard
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val startActivity = rememberStartActivity()
    var showLanguages by remember { mutableStateOf(false) }
    val chooserTitle = stringResource(R.string.set_email_chooser)
    val copiedMsg = stringResource(R.string.diag_copied)
    val noneMsg = stringResource(R.string.diag_none)

    fun openUrl(url: String) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.common_back)) }
            GradientText(stringResource(R.string.set_title), MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {

            // ---- voice enhancement ------------------------------------------------------------------------------------
            SectionHeader(stringResource(R.string.set_enhance))
            AuraCard {
                Text(stringResource(R.string.set_engine), style = MaterialTheme.typography.titleMedium)
                RadioRow(stringResource(R.string.set_engine_ai), s.engine == EnhanceEngine.AI) { vm.setEngine(EnhanceEngine.AI) }
                RadioRow(stringResource(R.string.set_engine_classic), s.engine == EnhanceEngine.CLASSIC) { vm.setEngine(EnhanceEngine.CLASSIC) }
                if (s.engine == EnhanceEngine.AI) {
                    SwitchRow(stringResource(R.string.set_fast_mode), stringResource(R.string.set_fast_mode_desc), s.fastMode, vm::setFastMode)
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.set_strength), style = MaterialTheme.typography.titleMedium)
                Slider(value = s.noiseStrength, onValueChange = vm::setStrength, valueRange = 0.2f..1f)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.set_strength_low), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.set_strength_high), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.set_keep_bg_value, (s.keepBackground * 100).roundToInt()), style = MaterialTheme.typography.titleMedium)
                Slider(value = s.keepBackground, onValueChange = vm::setKeepBackground, valueRange = 0f..0.6f)
                Text(stringResource(R.string.set_keep_bg_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            AuraCard {
                Text(stringResource(R.string.set_preset), style = MaterialTheme.typography.titleMedium)
                ChipRow(
                    listOf(
                        VoicePreset.OFF to stringResource(R.string.set_preset_off),
                        VoicePreset.NATURAL to stringResource(R.string.set_preset_natural),
                        VoicePreset.PODCAST to stringResource(R.string.set_preset_podcast),
                    ),
                    s.voicePreset, vm::setVoicePreset,
                )
                Text(stringResource(R.string.set_preset_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                SwitchRow(stringResource(R.string.set_normalize), stringResource(R.string.set_normalize_desc), s.loudnessBoost, vm::setLoudness)
                if (s.loudnessBoost) {
                    ChipRow(
                        listOf(
                            -19f to stringResource(R.string.set_loud_chat),
                            -16f to stringResource(R.string.set_loud_podcast),
                            -14f to stringResource(R.string.set_loud_loud),
                        ),
                        s.loudnessTarget, vm::setLoudnessTarget,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.set_cut), style = MaterialTheme.typography.titleMedium)
                ChipRow(
                    listOf(
                        SilenceCut.OFF to stringResource(R.string.set_cut_off),
                        SilenceCut.GENTLE to stringResource(R.string.set_cut_gentle),
                        SilenceCut.TIGHT to stringResource(R.string.set_cut_tight),
                    ),
                    s.silenceCut, vm::setSilenceCut,
                )
                Text(stringResource(R.string.set_cut_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.set_format), style = MaterialTheme.typography.titleMedium)
                RadioRow(stringResource(R.string.set_format_m4a), s.outputFormat == OutputFormat.M4A) { vm.setFormat(OutputFormat.M4A) }
                RadioRow(stringResource(R.string.set_format_opus), s.outputFormat == OutputFormat.OPUS, enabled = Build.VERSION.SDK_INT >= 29) { vm.setFormat(OutputFormat.OPUS) }
            }

            // ---- chat apps ----------------------------------------------------------------------------------------------
            SectionHeader(stringResource(R.string.set_apps))
            AuraCard {
                ChatApp.entries.forEach { app ->
                    SwitchRow(
                        stringResource(
                            when (app) {
                                ChatApp.WHATSAPP -> R.string.app_whatsapp
                                ChatApp.WHATSAPP_BUSINESS -> R.string.app_business
                                ChatApp.TELEGRAM -> R.string.app_telegram
                                ChatApp.INSTAGRAM -> R.string.app_instagram
                            }
                        ),
                        if (app == ChatApp.INSTAGRAM) stringResource(R.string.set_instagram_note) else null,
                        app in s.enabledApps,
                    ) { vm.setApp(app, it) }
                }
                SwitchRow(stringResource(R.string.set_auto_send), stringResource(R.string.set_auto_send_desc), s.autoSend, vm::setAutoSend)
                SwitchRow(stringResource(R.string.set_fallback_recent), stringResource(R.string.set_fallback_recent_desc), s.topRecentFallback, vm::setTopRecent)
                SwitchRow(stringResource(R.string.set_save_copy), null, s.saveCopy, vm::setSaveCopy)
            }

            SectionHeader(stringResource(R.string.set_overlay))
            AuraCard {
                Text(stringResource(R.string.set_size), style = MaterialTheme.typography.titleMedium)
                Slider(value = s.bubbleSizeDp.toFloat(), onValueChange = { vm.setBubbleSize(it.toInt()) }, valueRange = 48f..88f)
                SwitchRow(stringResource(R.string.set_haptics), null, s.haptics, vm::setHaptics)
                TextButton(onClick = vm::resetPosition) { Text(stringResource(R.string.set_reset_position)) }
            }

            SectionHeader(stringResource(R.string.set_general))
            AuraCard {
                NavRow(stringResource(R.string.set_language), s.language?.nativeName ?: "") { showLanguages = true }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.set_theme), style = MaterialTheme.typography.titleMedium)
                ChipRow(
                    listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.set_theme_system),
                        ThemeMode.LIGHT to stringResource(R.string.set_theme_light),
                        ThemeMode.DARK to stringResource(R.string.set_theme_dark),
                    ),
                    s.theme, vm::setTheme,
                )
                Spacer(Modifier.height(8.dp))
                NavRow(stringResource(R.string.set_battery), null) { startActivity(vm.batteryIntent()) }
            }

            SectionHeader(stringResource(R.string.set_about))
            AuraCard {
                NavRow(stringResource(R.string.set_privacy), null) { openUrl(Links.PRIVACY) }
                NavRow(stringResource(R.string.set_terms), null) { openUrl(Links.TERMS) }
                NavRow(stringResource(R.string.set_a11y_notice), null) { openUrl(Links.ACCESSIBILITY) }
                NavRow(stringResource(R.string.set_support), Links.SUPPORT_EMAIL) {
                    val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Links.SUPPORT_EMAIL}"))
                        .putExtra(Intent.EXTRA_SUBJECT, "AuraMic ${BuildConfig.VERSION_NAME}")
                    startActivity(Intent.createChooser(i, chooserTitle))
                }
                NavRow(stringResource(R.string.set_diag), null) {
                    Toast.makeText(context, if (vm.copyDiagnostics()) copiedMsg else noneMsg, Toast.LENGTH_SHORT).show()
                }
                Text(stringResource(R.string.set_diag_desc), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                stringResource(R.string.set_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
            )
        }
    }

    if (showLanguages) {
        AlertDialog(
            onDismissRequest = { showLanguages = false },
            confirmButton = { TextButton(onClick = { showLanguages = false }) { Text(stringResource(R.string.common_cancel)) } },
            title = { Text(stringResource(R.string.set_language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AppLanguage.entries.forEach { lang ->
                        LanguageCard(lang, lang == s.language) {
                            showLanguages = false
                            vm.setLanguage(lang)   // applied instantly by MainActivity
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun <T> ChipRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
        options.forEach { (value, label) ->
            FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(label, maxLines = 1) })
        }
    }
}

@Composable
private fun SwitchRow(title: String, desc: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (desc != null) Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun NavRow(title: String, value: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (!value.isNullOrEmpty()) Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
