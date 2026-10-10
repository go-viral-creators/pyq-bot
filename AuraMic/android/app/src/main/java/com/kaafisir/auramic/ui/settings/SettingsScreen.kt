package com.kaafisir.auramic.ui.settings

import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.kaafisir.auramic.domain.model.ThemeMode
import com.kaafisir.auramic.ui.components.AuraCard
import com.kaafisir.auramic.ui.components.GradientText
import com.kaafisir.auramic.ui.components.SectionHeader
import com.kaafisir.auramic.ui.components.rememberStartActivity
import com.kaafisir.auramic.ui.language.LanguageCard
import com.kaafisir.auramic.ui.pro.ProViewModel
import com.kaafisir.auramic.ui.pro.QualityPicker
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenPro: () -> Unit, vm: SettingsViewModel = hiltViewModel(), proVm: ProViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val pro by proVm.ui.collectAsStateWithLifecycle()
    val updateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}
    val latestMsg = stringResource(R.string.upd_latest)
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

            // ---- voice quality --------------------------------------------------------------------------------------
            SectionHeader(stringResource(R.string.q_title))
            QualityPicker(
                ui = pro,
                onPick = { if (!proVm.choose(it)) onOpenPro() },
                onGetPro = onOpenPro,
            )

            SectionHeader(stringResource(R.string.set_overlay))
            AuraCard {
                SwitchRow(stringResource(R.string.set_auto_send), stringResource(R.string.set_auto_send_desc), s.autoSend, vm::setAutoSend)
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
                NavRow(stringResource(R.string.set_check_update), null) {
                    proVm.checkUpdate { available ->
                        if (available) proVm.startUpdate(updateLauncher) else Toast.makeText(context, latestMsg, Toast.LENGTH_SHORT).show()
                    }
                }
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
