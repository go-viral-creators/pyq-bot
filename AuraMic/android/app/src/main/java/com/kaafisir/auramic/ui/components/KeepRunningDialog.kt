package com.kaafisir.auramic.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kaafisir.auramic.R
import com.kaafisir.auramic.data.permissions.KeepRunningGuide

/**
 * "Keep AuraMic running" steps for this phone's maker, with buttons that open the maker's Autostart page (best effort,
 * falls back to AuraMic's app info) and Android's battery-optimisation list. See [KeepRunningGuide].
 */
@Composable
fun KeepRunningDialog(guide: KeepRunningGuide.Guide, onOpenAutostart: () -> Unit, onOpenBattery: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.keep_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.keep_intro, guide.family), style = MaterialTheme.typography.bodyMedium)
                guide.steps.forEachIndexed { i, step ->
                    Row {
                        Text("${i + 1}.", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(24.dp))
                        Text(step, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    stringResource(R.string.keep_menu_note),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                TextButton(onClick = onOpenAutostart) { Text(stringResource(R.string.keep_open_autostart)) }
                TextButton(onClick = onOpenBattery) { Text(stringResource(R.string.keep_open_battery)) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } },
    )
}
