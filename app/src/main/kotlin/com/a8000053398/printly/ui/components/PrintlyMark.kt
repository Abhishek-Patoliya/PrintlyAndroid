package com.a8000053398.printly.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * Printly's brand symbol, ported 1:1 from iOS's `PageEditGlyph`: a portrait
 * page with two holes cut into it (even-odd fill) — a photo/layout window
 * near the top, and a short diagonal tick below it standing in for an
 * active edit. One unified silhouette; never restyled per screen, only scaled.
 */
@Composable
fun PrintlyMark(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 40.dp, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier = modifier.size(size)) {
        val s = min(this.size.width, this.size.height)
        val pageWidth = s * 0.60f
        val pageHeight = s * 0.84f
        val pageX = (this.size.width - pageWidth) / 2
        val pageY = (this.size.height - pageHeight) / 2
        val pageCorner = pageWidth * 0.16f

        val path = Path().apply {
            addOutline(
                androidx.compose.ui.graphics.Outline.Rounded(
                    androidx.compose.ui.geometry.RoundRect(
                        Rect(Offset(pageX, pageY), Size(pageWidth, pageHeight)),
                        CornerRadius(pageCorner, pageCorner)
                    )
                )
            )

            // Photo/layout window: the upper portion of the page.
            val windowInset = pageWidth * 0.15f
            val windowRect = Rect(
                Offset(pageX + windowInset, pageY + pageHeight * 0.14f),
                Size(pageWidth - windowInset * 2, pageHeight * 0.36f)
            )
            val windowCorner = pageCorner * 0.55f
            addOutline(
                androidx.compose.ui.graphics.Outline.Rounded(
                    androidx.compose.ui.geometry.RoundRect(windowRect, CornerRadius(windowCorner, windowCorner))
                )
            )

            // Edit tick: a short diagonal mark in the lower portion of the page.
            val tickWidth = pageWidth * 0.5f
            val tickThickness = pageWidth * 0.16f
            val tickCenterX = pageX + pageWidth * 0.5f
            val tickCenterY = pageY + pageHeight * 0.72f
            val tickPath = Path().apply {
                addOutline(
                    androidx.compose.ui.graphics.Outline.Rounded(
                        androidx.compose.ui.geometry.RoundRect(
                            Rect(Offset(-tickWidth / 2, -tickThickness / 2), Size(tickWidth, tickThickness)),
                            CornerRadius(tickThickness / 2, tickThickness / 2)
                        )
                    )
                )
            }
            val matrix = androidx.compose.ui.graphics.Matrix().apply {
                translate(tickCenterX, tickCenterY)
                rotateZ(-180f / 7f)
            }
            tickPath.transform(matrix)
            addPath(tickPath)

            fillType = PathFillType.EvenOdd
        }

        drawPath(path, color = color)
    }
}

/** The full brand lockup — symbol plus wordmark. */
@Composable
fun PrintlyBrandLockup(modifier: Modifier = Modifier, markSize: androidx.compose.ui.unit.Dp = 34.dp, wordmarkSize: androidx.compose.ui.unit.TextUnit = 28.sp) {
    Row(modifier = modifier, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        PrintlyMark(size = markSize)
        androidx.compose.foundation.layout.Spacer(Modifier.size((markSize.value * 0.34f).dp))
        Text("Printly", fontSize = wordmarkSize, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
