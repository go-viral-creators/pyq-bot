package com.kaafisir.auramic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import com.kaafisir.auramic.ui.theme.AuraCyan
import com.kaafisir.auramic.ui.theme.AuraGradient
import com.kaafisir.auramic.ui.theme.AuraViolet

/** Primary call-to-action with the brand gradient. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    brush: Brush = AuraGradient,
) {
    Box(
        modifier
            .heightIn(min = 54.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(18.dp))
            .background(brush)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp))
        }
    }
}


@Composable
fun AuraCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderBrush: Brush? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val border = borderBrush?.let { BorderStroke(1.5.dp, it) } ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val base = modifier.fillMaxWidth()
    if (onClick != null) {
        Surface(onClick = onClick, modifier = base, shape = shape, color = MaterialTheme.colorScheme.surfaceContainer, border = border) {
            Column(Modifier.padding(18.dp), content = content)
        }
    } else {
        Surface(modifier = base, shape = shape, color = MaterialTheme.colorScheme.surfaceContainer, border = border) {
            Column(Modifier.padding(18.dp), content = content)
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier.padding(start = 6.dp, top = 22.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
    )
}

@Composable
fun GradientText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    Text(text, modifier, style = style.copy(brush = remember { Brush.linearGradient(listOf(AuraViolet, AuraCyan)) }))
}

/** Rounded square with an icon, tinted by [tint]. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 44.dp) {
    Box(modifier.size(size).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun StatusDot(on: Boolean, modifier: Modifier = Modifier) {
    val color by animateColorAsState(if (on) com.kaafisir.auramic.ui.theme.AuraMint else MaterialTheme.colorScheme.outline, label = "dot")
    Box(modifier.size(10.dp).background(color, CircleShape))
}
