package com.kaafisir.auramic.ui.language

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaafisir.auramic.R
import com.kaafisir.auramic.domain.model.AppLanguage
import com.kaafisir.auramic.ui.components.GradientButton
import com.kaafisir.auramic.ui.components.GradientText
import com.kaafisir.auramic.ui.theme.AuraGradient
import java.util.Locale

@Composable
fun LanguageScreen(initial: AppLanguage, onConfirm: (AppLanguage) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(initial) }
    val base = LocalContext.current
    val preview = remember(selected) { com.kaafisir.auramic.core.LocaleManager.localized(base, selected) }

    // Everything inside renders in the *selected* language, so tapping a card previews it live.
    CompositionLocalProvider(LocalContext provides preview, LocalConfiguration provides preview.resources.configuration) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(28.dp))
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(AuraGradient),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.GraphicEq, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
            Spacer(Modifier.height(20.dp))
            GradientText(stringResource(R.string.lang_title), MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.lang_subtitle),
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(AppLanguage.entries) { lang ->
                    LanguageCard(lang, lang == selected) { selected = lang }
                }
            }
            Spacer(Modifier.height(12.dp))
            GradientButton(stringResource(R.string.common_continue), { onConfirm(selected) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun LanguageCard(lang: AppLanguage, selected: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "b")
    val width by animateDpAsState(if (selected) 2.dp else 1.dp, label = "w")
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainer, label = "c",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .border(width, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(AuraGradient), contentAlignment = Alignment.Center) {
            Text(lang.badge, color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (lang.badge.length > 3) 12.sp else 16.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(lang.nativeName, style = MaterialTheme.typography.titleMedium)
            Text(lang.englishName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) {
            Box(Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
