package com.kaafisir.auramic.ui.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaafisir.auramic.R
import com.kaafisir.auramic.data.update.UpdateState
import com.kaafisir.auramic.domain.model.Quality
import com.kaafisir.auramic.ui.components.AuraCard
import com.kaafisir.auramic.ui.components.GradientButton
import com.kaafisir.auramic.ui.components.IconBadge
import com.kaafisir.auramic.ui.theme.AuraAmber
import com.kaafisir.auramic.ui.theme.AuraGradient
import com.kaafisir.auramic.ui.theme.AuraMint
import com.kaafisir.auramic.ui.theme.AuraViolet

/** The one choice a user sees: Medium (free, default) or High (Pro). */
@Composable
fun QualityPicker(ui: ProUi, onPick: (Quality) -> Unit, onGetPro: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        QualityCard(
            title = stringResource(R.string.q_medium), desc = stringResource(R.string.q_medium_desc),
            badge = stringResource(R.string.q_badge_free), badgeColor = AuraMint,
            selected = ui.quality == Quality.MEDIUM, onClick = { onPick(Quality.MEDIUM) },
        )
        QualityCard(
            title = stringResource(R.string.q_high), desc = stringResource(R.string.q_high_desc),
            badge = stringResource(R.string.q_badge_pro), badgeColor = AuraAmber,
            selected = ui.quality == Quality.HIGH, onClick = { onPick(Quality.HIGH) },
        )
        if (ui.pro.subscribed) {
            Text(stringResource(R.string.q_pro_active), style = MaterialTheme.typography.bodyMedium, color = AuraMint)
        } else if (ui.pro.pro) {
            Text(stringResource(R.string.pro_grant_until, formatDay(ui.pro.grantUntil ?: 0)), style = MaterialTheme.typography.bodyMedium, color = AuraMint)
        } else {
            Text(
                if (ui.pro.trialLeft > 0) stringResource(R.string.q_trial_left, ui.pro.trialLeft) else stringResource(R.string.q_trial_over),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GradientButton(stringResource(R.string.q_get_pro), onGetPro, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun QualityCard(title: String, desc: String, badge: String, badgeColor: Color, selected: Boolean, onClick: () -> Unit) {
    AuraCard(onClick = onClick, borderBrush = if (selected) AuraGradient else null) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.padding(top = 2.dp).size(26.dp).clip(CircleShape)
                    .background(if (selected) AuraViolet else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) { if (selected) Icon(Icons.Rounded.Check, stringResource(R.string.q_selected), tint = Color.White, modifier = Modifier.size(16.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                    Text(
                        badge, fontSize = 11.sp, color = badgeColor, maxLines = 1, softWrap = false,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(badgeColor.copy(alpha = 0.16f)).padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Shown when Google Play has a newer version. */
@Composable
fun UpdateBanner(state: UpdateState, onUpdate: () -> Unit, onRestart: () -> Unit, modifier: Modifier = Modifier) {
    if (state == UpdateState.NONE) return
    AuraCard(modifier, borderBrush = SolidColor(AuraMint.copy(alpha = 0.6f))) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(Icons.Rounded.SystemUpdate, AuraMint)
            Text(
                stringResource(
                    when (state) {
                        UpdateState.DOWNLOADING -> R.string.upd_downloading
                        UpdateState.READY -> R.string.upd_ready
                        else -> R.string.upd_title
                    },
                ),
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
            )
            when (state) {
                UpdateState.AVAILABLE -> TextButton(onClick = onUpdate) { Text(stringResource(R.string.upd_btn)) }
                UpdateState.READY -> TextButton(onClick = onRestart) { Text(stringResource(R.string.upd_restart)) }
                else -> Spacer(Modifier.height(1.dp))
            }
        }
    }
}

/** Last day of a grant (expiry is the end of that UTC day), in the user's locale. */
fun formatDay(endMillis: Long): String =
    java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        .format(java.util.Date(endMillis - 1))
