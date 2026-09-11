package com.a8000053398.printly.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.a8000053398.printly.core.PointD
import com.a8000053398.printly.model.PlacedPhoto
import com.a8000053398.printly.service.SnapGuideService
import com.a8000053398.printly.service.SnapGuides
import com.a8000053398.printly.service.layout.CutGuideGeometry
import com.a8000053398.printly.viewmodel.ProjectEditorViewModel
import kotlin.math.min

/**
 * Renders the printable page to scale on screen and lets the user drag
 * individual placements — a Kotlin/Compose port of iOS's `PageCanvasView`.
 * All geometry is computed in physical points by `PageLayoutEngine`; this
 * composable only maps those points to screen pixels via a single `scale`.
 */
@Composable
fun PageCanvas(viewModel: ProjectEditorViewModel, onTapEmptyArea: () -> Unit, modifier: Modifier = Modifier) {
    val layout = viewModel.layout
    val pageSize = layout.pageSize
    var canvasSizePx by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var snapGuides by remember { mutableStateOf<SnapGuides?>(null) }

    val aspect = (pageSize.width / kotlin.math.max(pageSize.height, 1.0)).toFloat()

    Box(
        modifier = modifier
            .aspectRatio(aspect)
            .onSizeChanged { canvasSizePx = it }
    ) {
        val widthPx = canvasSizePx.width.toFloat().coerceAtLeast(1f)
        val heightPx = canvasSizePx.height.toFloat().coerceAtLeast(1f)
        val scale = min(widthPx / pageSize.width.toFloat().coerceAtLeast(1f), heightPx / pageSize.height.toFloat().coerceAtLeast(1f))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(viewModel.project.backgroundColor.color)
                .pointerInput(viewModel.project.photos.isEmpty()) {
                    detectTapGestures {
                        if (viewModel.project.photos.isEmpty()) onTapEmptyArea() else viewModel.selectedPlacementID = null
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val printable = layout.printableRect
                drawRect(
                    color = Color(0xFF0065E9).copy(alpha = 0.5f),
                    topLeft = Offset((printable.x * scale).toFloat(), (printable.y * scale).toFloat()),
                    size = Size((printable.width * scale).toFloat(), (printable.height * scale).toFloat()),
                    style = Stroke(width = 1f)
                )
                if (viewModel.project.showCutGuides) {
                    for (segment in CutGuideGeometry.segments(layout.placements)) {
                        drawLine(
                            color = Color.Gray,
                            start = Offset((segment.start.x * scale).toFloat(), (segment.start.y * scale).toFloat()),
                            end = Offset((segment.end.x * scale).toFloat(), (segment.end.y * scale).toFloat()),
                            strokeWidth = 1f
                        )
                    }
                }
                snapGuides?.verticalX?.let { x ->
                    drawLine(Color(0xFF0065E9), Offset((x * scale).toFloat(), 0f), Offset((x * scale).toFloat(), heightPx), strokeWidth = 1.5f)
                }
                snapGuides?.horizontalY?.let { y ->
                    drawLine(Color(0xFF0065E9), Offset(0f, (y * scale).toFloat()), Offset(widthPx, (y * scale).toFloat()), strokeWidth = 1.5f)
                }
            }

            for (placement in layout.placements) {
                key(placement.id) {
                    PlacementView(viewModel = viewModel, placement = placement, scale = scale, onSnapGuidesChanged = { snapGuides = it })
                }
            }

            if (viewModel.project.photos.isEmpty()) {
                Text(
                    "Tap Photos to add your first photo",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun key(key: Any, content: @Composable () -> Unit) = androidx.compose.runtime.key(key) { content() }

@Composable
private fun PlacementView(
    viewModel: ProjectEditorViewModel,
    placement: PlacedPhoto,
    scale: Float,
    onSnapGuidesChanged: (SnapGuides?) -> Unit
) {
    val isSelected = viewModel.selectedPlacementID == placement.id
    val isMultiSelected = viewModel.multiSelectedPlacementIDs.contains(placement.id)
    val isHighlighted = isSelected || isMultiSelected

    val displayWidthPx = placement.size.width * scale
    val displayHeightPx = placement.size.height * scale
    val centerXPx = placement.center.x * scale
    val centerYPx = placement.center.y * scale

    val bitmap = remember(placement.photoID, viewModel.project) { viewModel.image(placement.photoID) }
    var dragCurrentOrigin by remember(placement.id) { mutableStateOf<PointD?>(null) }

    val density = LocalDensity.current
    val widthDp = with(density) { displayWidthPx.toFloat().toDp() }
    val heightDp = with(density) { displayHeightPx.toFloat().toDp() }

    Box(
        modifier = Modifier
            .offset { androidx.compose.ui.unit.IntOffset((centerXPx - displayWidthPx / 2).toInt(), (centerYPx - displayHeightPx / 2).toInt()) }
            .size(widthDp, heightDp)
            .rotate(placement.placementRotationDegrees.toFloat())
            .then(if (isHighlighted) Modifier.border(2.dp, MaterialTheme.colorScheme.primary) else Modifier)
            .pointerInput(placement.id, viewModel.isSelectingMultiplePlacements) {
                detectTapGestures(onTap = {
                    if (viewModel.isSelectingMultiplePlacements) viewModel.toggleMultiSelection(placement.id)
                    else viewModel.selectedPlacementID = placement.id
                })
            }
            .pointerInput(placement.id, viewModel.isSelectingMultiplePlacements, scale) {
                if (!viewModel.isSelectingMultiplePlacements) {
                    detectDragGestures(
                        onDragStart = {
                            dragCurrentOrigin = placement.origin
                            viewModel.beginBatch()
                            viewModel.selectedPlacementID = placement.id
                        },
                        onDragEnd = {
                            dragCurrentOrigin = null
                            onSnapGuidesChanged(null)
                            viewModel.endBatch()
                        },
                        onDragCancel = {
                            dragCurrentOrigin = null
                            onSnapGuidesChanged(null)
                            viewModel.endBatch()
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val current = dragCurrentOrigin ?: placement.origin
                        val proposed = PointD(current.x + dragAmount.x / scale, current.y + dragAmount.y / scale)
                        dragCurrentOrigin = proposed
                        val result = SnapGuideService.snappedOrigin(
                            proposedOrigin = proposed,
                            size = placement.size,
                            printableRect = viewModel.layout.printableRect,
                            otherPlacements = viewModel.layout.placements.filter { it.id != placement.id },
                            threshold = (SnapGuideService.DEFAULT_THRESHOLD * 1.6) / scale.coerceAtLeast(0.01f)
                        )
                        onSnapGuidesChanged(result.guides)
                        viewModel.movePlacement(placement.id, result.origin)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
        } else {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
        }
        if (viewModel.isSelectingMultiplePlacements && isMultiSelected) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)))
        }
    }
}
