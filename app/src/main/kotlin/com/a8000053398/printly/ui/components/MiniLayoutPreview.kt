package com.a8000053398.printly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.a8000053398.printly.core.RectD
import com.a8000053398.printly.core.SizeD
import com.a8000053398.printly.model.PageMargins
import com.a8000053398.printly.model.PageSize
import com.a8000053398.printly.model.PageSpacing
import com.a8000053398.printly.model.PhotoSize
import com.a8000053398.printly.model.PrintProject
import com.a8000053398.printly.model.PrintTemplate
import com.a8000053398.printly.service.layout.PageLayoutEngine
import java.util.UUID
import kotlin.math.min

/**
 * A miniature, geometrically-accurate preview of a page's photo placements —
 * computed via the same [PageLayoutEngine] the real editor canvas and
 * PDF/image export use, so a template/project/setting preview always shows
 * its *actual* layout rather than an approximation.
 */
@Composable
fun MiniLayoutPreview(
    pageSize: SizeD,
    placements: List<RectD>,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val scale = min(this.size.width / maxOf(pageSize.width.toFloat(), 1f), this.size.height / maxOf(pageSize.height.toFloat(), 1f))
        val displayWidth = pageSize.width.toFloat() * scale
        val displayHeight = pageSize.height.toFloat() * scale
        val offsetX = (this.size.width - displayWidth) / 2
        val offsetY = (this.size.height - displayHeight) / 2

        drawRoundRect(
            color = Color.White,
            topLeft = Offset(offsetX, offsetY),
            size = Size(displayWidth, displayHeight),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.1f),
            topLeft = Offset(offsetX, offsetY),
            size = Size(displayWidth, displayHeight),
            cornerRadius = CornerRadius(3f, 3f),
            style = Stroke(width = 0.75f)
        )

        for (rect in placements) {
            val slotWidth = maxOf(1f, rect.width.toFloat() * scale - 0.5f)
            val slotHeight = maxOf(1f, rect.height.toFloat() * scale - 0.5f)
            val left = offsetX + rect.x.toFloat() * scale
            val top = offsetY + rect.y.toFloat() * scale
            val slotRadius = maxOf(0.5f, min(2.1f, min(slotWidth, slotHeight) * 0.16f))

            drawRoundRect(
                color = tint.copy(alpha = 0.22f),
                topLeft = Offset(left, top),
                size = Size(slotWidth, slotHeight),
                cornerRadius = CornerRadius(slotRadius, slotRadius)
            )
            drawRoundRect(
                color = tint.copy(alpha = 0.6f),
                topLeft = Offset(left, top),
                size = Size(slotWidth, slotHeight),
                cornerRadius = CornerRadius(slotRadius, slotRadius),
                style = Stroke(width = 0.75f)
            )
        }
    }
}

/** Computes real placements for arbitrary page/margins/photo/spacing
 * settings — the general-purpose entry point for live previews while the
 * user is still adjusting a setting. */
fun layoutPreviewPlacements(
    pageSize: PageSize,
    margins: PageMargins,
    photoSize: PhotoSize,
    spacing: PageSpacing,
    count: Int = 40,
    fillPageEdgeToEdge: Boolean = false
): Pair<SizeD, List<RectD>> {
    val resolvedPageSize = pageSize.pointSize
    val layout = PageLayoutEngine.layout(
        pageSize = resolvedPageSize,
        margins = margins,
        photoSize = photoSize.pointSize,
        spacing = spacing,
        photoSequence = List(maxOf(1, count)) { UUID.randomUUID() },
        fillPageEdgeToEdge = fillPageEdgeToEdge
    )
    return resolvedPageSize to layout.placements.map { RectD.of(it.origin, it.size) }
}

fun templatePreview(template: PrintTemplate): Pair<SizeD, List<RectD>> = layoutPreviewPlacements(
    pageSize = template.pageSize, margins = template.margins, photoSize = template.photoSize,
    spacing = template.spacing, fillPageEdgeToEdge = template.fillPageEdgeToEdge
)

fun projectPreview(project: PrintProject): Pair<SizeD, List<RectD>> = layoutPreviewPlacements(
    pageSize = project.pageSize, margins = project.margins, photoSize = project.photoSize,
    spacing = project.spacing, fillPageEdgeToEdge = project.fillPageEdgeToEdge
)

/** Wraps content (typically [MiniLayoutPreview]) in the same rounded-square
 * footprint [IconBadge] uses, so a real visual preview and a generic icon
 * are always visually interchangeable in a card's leading-icon slot. */
fun Modifier.previewBadgeContainer(size: androidx.compose.ui.unit.Dp = 44.dp, tint: Color): Modifier = this
    .size(size)
    .clip(RoundedCornerShape((size.value * 0.32f).dp))
    .background(tint.copy(alpha = 0.14f))
    .padding((size.value * 0.18f).dp)
