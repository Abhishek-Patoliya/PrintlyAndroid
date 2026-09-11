package com.a8000053398.printly.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.graphicsLayer
import com.a8000053398.printly.ui.theme.Metrics

/** Rounded-card container: background, corner radius, and a soft shadow — the
 * direct port of iOS `CardBackground`/`cardStyle()`. */
@Composable
fun Modifier.cardStyle(cornerRadius: androidx.compose.ui.unit.Dp = Metrics.radiusMedium.dp, shadow: Boolean = true): Modifier = this
    .then(if (shadow) Modifier.shadow(4.dp, RoundedCornerShape(cornerRadius), clip = false) else Modifier)
    .clip(RoundedCornerShape(cornerRadius))
    .background(MaterialTheme.colorScheme.surface)

/** Primary call-to-action: filled with the accent color, gentle press-scale feedback. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDestructive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "primaryButtonScale")
    val bg = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(Metrics.radiusMedium.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.4f))
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/** Secondary action: tinted, subtle fill, same rounded language as primary. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "secondaryButtonScale")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(Metrics.radiusMedium.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/** Round glass icon button, used for compact toolbar/top-bar actions. */
@Composable
fun IconGlassButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 36.dp, tint: Color = Color.Unspecified) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "iconGlassScale")

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** A small rounded icon badge (colored square/circle behind an icon), used
 * consistently for list-row leading icons across Home/Templates/sheets. */
@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary, size: androidx.compose.ui.unit.Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape((size.value * 0.32f).dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.42f))
    }
}

/** `IconBadge`'s special-purpose sibling for passport/visa-sized photos — the
 * same rounded light-blue container, plus a photo glyph with a crisp square
 * frame outline layered on top, signalling "a photo held to one fixed size." */
@Composable
fun PassportPhotoIconBadge(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary, size: androidx.compose.ui.unit.Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape((size.value * 0.32f).dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Photo, contentDescription = null, tint = tint.copy(alpha = 0.45f), modifier = Modifier.size(size * 0.3f))
        androidx.compose.foundation.Canvas(modifier = Modifier.size(size * 0.48f)) {
            val strokeWidth = (size.toPx() * 0.05f).coerceAtLeast(1.5f)
            val cornerRadius = size.toPx() * 0.06f
            drawRoundRect(
                color = tint,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
        }
    }
}

/** Consistent section header used above grouped content on custom (non-Form) screens. */
@Composable
fun SectionHeaderText(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** A tasteful empty-photo placeholder, used wherever a photo slot has no image yet. */
@Composable
fun PhotoSlotPlaceholder(modifier: Modifier = Modifier, cornerRadius: androidx.compose.ui.unit.Dp = Metrics.radiusSmall.dp) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Photo, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}
