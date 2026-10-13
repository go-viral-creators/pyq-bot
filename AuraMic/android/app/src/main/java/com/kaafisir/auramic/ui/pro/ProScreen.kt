package com.kaafisir.auramic.ui.pro

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaafisir.auramic.R
import com.kaafisir.auramic.data.pro.Plan
import com.kaafisir.auramic.data.pro.PlayProAccess
import com.kaafisir.auramic.ui.components.AuraCard
import com.kaafisir.auramic.ui.components.GradientButton
import com.kaafisir.auramic.ui.components.GradientText
import com.kaafisir.auramic.ui.theme.AuraAmber
import com.kaafisir.auramic.ui.theme.AuraGradient
import com.kaafisir.auramic.ui.theme.AuraMint
import com.kaafisir.auramic.ui.theme.AuraViolet

private const val MANAGE_URL = "https://play.google.com/store/account/subscriptions?sku=${PlayProAccess.PRODUCT_ID}&package=com.kaafisir.auramic"

@Composable
fun ProScreen(onBack: () -> Unit, vm: ProViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current
    var plan by remember { mutableStateOf(Plan.YEARLY) }
    val unavailable = stringResource(R.string.pro_unavailable)
    var showRedeem by remember { mutableStateOf(false) }

    // A purchase finished while this screen was open: switch to High Quality and close.
    val wasSubscribed = remember { ui.pro.subscribed }
    LaunchedEffect(ui.pro.subscribed) {
        if (ui.pro.subscribed && !wasSubscribed) { vm.onSubscribed(); onBack() }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.common_back)) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            GradientText(stringResource(R.string.pro_title), MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.pro_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))

            AuraCard {
                listOf(R.string.pro_b1, R.string.pro_b2, R.string.pro_b3, R.string.pro_b4).forEach { res ->
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.padding(top = 1.dp).size(22.dp).clip(CircleShape).background(AuraMint), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        Text(stringResource(res), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            if (ui.pro.pro && !ui.pro.subscribed) {
                // Free Pro from a redeemed code. Plans stay available below (a subscription simply takes over).
                Text(stringResource(R.string.pro_grant_until, formatDay(ui.pro.grantUntil ?: 0)), style = MaterialTheme.typography.titleLarge, color = AuraMint, textAlign = TextAlign.Center)
                TextButton(onClick = { vm.removeGrant() }) { Text(stringResource(R.string.pro_grant_remove), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Spacer(Modifier.height(8.dp))
            }
            if (ui.pro.subscribed) {
                Text(stringResource(R.string.pro_active_title), style = MaterialTheme.typography.titleLarge, color = AuraMint, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                GradientButton(stringResource(R.string.pro_manage), {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MANAGE_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }, Modifier.fillMaxWidth())
            } else {
                val offers = ui.pro.offers.associateBy { it.plan }
                listOf(Plan.YEARLY, Plan.MONTHLY).forEach { p ->
                    PlanCard(
                        title = stringResource(if (p == Plan.YEARLY) R.string.pro_yearly else R.string.pro_monthly),
                        price = offers[p]?.price ?: "—",
                        per = stringResource(if (p == Plan.YEARLY) R.string.pro_per_year else R.string.pro_per_month),
                        best = p == Plan.YEARLY,
                        selected = plan == p, onClick = { plan = p },
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (offers.isEmpty()) {
                    Text(unavailable, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                }
                GradientButton(
                    stringResource(R.string.pro_subscribe),
                    { if (activity == null || !vm.buy(activity, plan)) Toast.makeText(context, unavailable, Toast.LENGTH_LONG).show() },
                    Modifier.fillMaxWidth(), enabled = offers.isNotEmpty(),
                )
                Text(stringResource(R.string.pro_renews), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                TextButton(onClick = { vm.refresh() }) { Text(stringResource(R.string.pro_restore)) }
                TextButton(onClick = { showRedeem = true }) { Text(stringResource(R.string.pro_redeem)) }
                TextButton(onClick = onBack) { Text(stringResource(R.string.pro_free), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (showRedeem) RedeemDialog(vm) { showRedeem = false }
}

@Composable
private fun PlanCard(title: String, price: String, per: String, best: Boolean, selected: Boolean, onClick: () -> Unit) {
    AuraCard(onClick = onClick, borderBrush = if (selected) AuraGradient else null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.size(26.dp).clip(CircleShape).background(if (selected) AuraViolet else MaterialTheme.colorScheme.surfaceVariant), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (best) Text(
                        stringResource(R.string.pro_best), fontSize = 11.sp, color = AuraAmber, maxLines = 1, softWrap = false,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(AuraAmber.copy(alpha = 0.16f)).padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
                Text(per, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(price, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun RedeemDialog(vm: ProViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var requestCode by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { requestCode = vm.requestCode() }
    val copied = stringResource(R.string.common_copied)
    val msgBad = stringResource(R.string.pro_redeem_bad)
    val msgExpired = stringResource(R.string.pro_redeem_expired)
    val msgOther = stringResource(R.string.pro_redeem_other_phone)
    val msgRevoked = stringResource(R.string.pro_redeem_revoked)
    val okTemplate = stringResource(R.string.pro_redeem_ok)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pro_redeem_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = code, onValueChange = { code = it; message = null },
                    label = { Text(stringResource(R.string.pro_redeem_field)) },
                    singleLine = false, maxLines = 4, modifier = Modifier.fillMaxWidth(), enabled = !ok,
                )
                message?.let { Text(it, color = if (ok) AuraMint else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                if (requestCode.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.pro_request_code, requestCode), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("AuraMic request code", requestCode))
                            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                        }) { Text(stringResource(R.string.common_copy)) }
                    }
                    Text(stringResource(R.string.pro_request_code_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            if (ok) TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
            else TextButton(enabled = code.isNotBlank() && !busy, onClick = {
                busy = true
                vm.redeem(code) { r ->
                    busy = false
                    ok = r is com.kaafisir.auramic.data.pro.GrantCheck.Valid
                    message = when (r) {
                        is com.kaafisir.auramic.data.pro.GrantCheck.Valid -> okTemplate.format(formatDay(r.grant.expiresAtMillis))
                        com.kaafisir.auramic.data.pro.GrantCheck.Expired -> msgExpired
                        com.kaafisir.auramic.data.pro.GrantCheck.OtherPhone -> msgOther
                        com.kaafisir.auramic.data.pro.GrantCheck.Revoked -> msgRevoked
                        else -> msgBad
                    }
                }
            }) { Text(stringResource(R.string.pro_redeem_button)) }
        },
        dismissButton = { if (!ok) TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
