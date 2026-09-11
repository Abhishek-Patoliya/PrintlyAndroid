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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a8000053398.printly.core.PointD
import com.a8000053398.printly.core.RectD
import com.a8000053398.printly.core.SizeD
import com.a8000053398.printly.model.PlacedPhoto
import com.a8000053398.printly.service.SnapGuideService
import com.a8000053398.printly.service.SnapGuides
import com.a8000053398.printly.service.layout.CutGuideGeometry
import com.a8000053398.printly.service.layout.ResizeGeometryService
import com.a8000053398.printly.service.layout.ResizeHandle
import com.a8000053398.printly.service.layout.RotationMath
import com.a8000053398.printly.util.MeasurementFormatter
import com.a8000053398.printly.util.PrintlyConstants
import com.a8000053398.printly.viewmodel.ProjectEditorViewModel
import java.util.UUID
import kotlin.math.min

/**
 * Renders the printable page to scale on screen and lets the user drag,
 * resize, and rotate individual placements — a Kotlin/Compose port of iOS's
 * `PageCanvasView`. All geometry is computed in physical points by
 * `PageLayoutEngine`; this composable only maps those points to screen
 * pixels via a single `scale` factor, exactly like the iOS canvas.
 */
@Composable
fun PageCanvas(viewModel: ProjectEditorViewModel, onTapEmptyArea: () -> Unit, modifier: Modifier = Modifier) {
    val layout = viewModel.layout
    val pageSize = layout.pageSize
    var canvasSizePx by remember { mutableStateOf(IntSize.Zero) }
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

            if (!viewModel.isSelectingMultiplePlacements) {
                val selected = layout.placements.firstOrNull { it.id == viewModel.selectedPlacementID }
                if (selected != null) {
                    key(selected.id) {
                        ResizeHandlesOverlay(viewModel = viewModel, placement = selected, scale = scale)
                    }
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
            .offset { IntOffset((centerXPx - displayWidthPx / 2).toInt(), (centerYPx - displayHeightPx / 2).toInt()) }
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

// MARK: - Resize & rotate handles

private const val HANDLE_TOUCH_TARGET_DP = 40
private const val HANDLE_DOT_DIAMETER_DP = 14
private const val ROTATE_HANDLE_GAP_SCREEN_DP = 28

/** Converts a point offset from a placement's own (unrotated) center — e.g. a
 * corner or edge midpoint of its footprint — into on-screen pixels, rotating
 * it by the placement's `placementRotationDegrees` first. Every handle sits
 * exactly where the placement's rotated border actually is, matching
 * `PlacementView`'s own `.rotate()`. */
private fun rotatedScreenPosition(localOffset: PointD, placement: PlacedPhoto, scale: Float): Offset {
    val rotated = RotationMath.rotate(localOffset, placement.placementRotationDegrees)
    return Offset(((placement.center.x + rotated.x) * scale).toFloat(), ((placement.center.y + rotated.y) * scale).toFloat())
}

private fun handleLocalOffset(handle: ResizeHandle, placement: PlacedPhoto): PointD =
    PointD((handle.fractionX - 0.5) * placement.size.width, (handle.fractionY - 0.5) * placement.size.height)

/** Draggable resize + rotate handles for the currently selected placement,
 * plus live width x height / angle readouts while a handle is being
 * dragged — a Kotlin/Compose port of iOS's `resizeHandlesOverlay`. */
@Composable
private fun ResizeHandlesOverlay(viewModel: ProjectEditorViewModel, placement: PlacedPhoto, scale: Float) {
    // Precompute every density-dependent pixel value once here, in composable
    // scope — `Modifier.offset { ... }`'s lambda runs during layout, not
    // composition, so it cannot call `LocalDensity.current` itself.
    val density = LocalDensity.current
    val touchTargetHalfPx = with(density) { (HANDLE_TOUCH_TARGET_DP.dp / 2).toPx() }
    val rotateGapPx = with(density) { ROTATE_HANDLE_GAP_SCREEN_DP.dp.toPx() }
    val labelGapPx = with(density) { 22.dp.toPx() }

    var resizingHandle by remember(placement.id) { mutableStateOf<ResizeHandle?>(null) }
    var resizeStartFrame by remember(placement.id) { mutableStateOf<RectD?>(null) }
    var liveFrame by remember(placement.id) { mutableStateOf<RectD?>(null) }

    var isRotating by remember(placement.id) { mutableStateOf(false) }
    var rotateCurrentScreen by remember(placement.id) { mutableStateOf<Offset?>(null) }
    var liveRotationDegrees by remember(placement.id) { mutableStateOf<Double?>(null) }

    for (handle in ResizeHandle.entries) {
        var cumulativeTranslation by remember(placement.id, handle) { mutableStateOf(PointD.zero) }
        val position = rotatedScreenPosition(handleLocalOffset(handle, placement), placement, scale)
        Box(
            modifier = Modifier
                .offset { IntOffset((position.x - touchTargetHalfPx).toInt(), (position.y - touchTargetHalfPx).toInt()) }
                .size(HANDLE_TOUCH_TARGET_DP.dp)
                .pointerInput(placement.id, handle, scale) {
                    detectDragGestures(
                        onDragStart = {
                            resizeStartFrame = placement.frame
                            liveFrame = placement.frame
                            cumulativeTranslation = PointD.zero
                            resizingHandle = handle
                            viewModel.beginBatch()
                        },
                        onDragEnd = {
                            resizeStartFrame = null
                            liveFrame = null
                            resizingHandle = null
                            viewModel.endBatch()
                        },
                        onDragCancel = {
                            resizeStartFrame = null
                            liveFrame = null
                            resizingHandle = null
                            viewModel.endBatch()
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val startFrame = resizeStartFrame ?: return@detectDragGestures
                        // `dragAmount` is only the delta since the last frame (unlike SwiftUI's
                        // DragGesture.translation, which is cumulative) — accumulate it ourselves
                        // in raw (unrotated, page-space) coordinates before derotating, mirroring
                        // iOS's resizeGesture exactly.
                        cumulativeTranslation = PointD(
                            cumulativeTranslation.x + dragAmount.x / scale,
                            cumulativeTranslation.y + dragAmount.y / scale
                        )
                        val localTranslation = RotationMath.derotate(cumulativeTranslation, placement.placementRotationDegrees)
                        val newFrame = ResizeGeometryService.resizedFrame(
                            handle = handle,
                            startFrame = startFrame,
                            translation = SizeD(localTranslation.x, localTranslation.y),
                            pageSize = viewModel.layout.pageSize,
                            minSide = PrintlyConstants.minResizeDimensionPoints,
                            maxSide = PrintlyConstants.maxResizeDimensionPoints
                        )
                        liveFrame = newFrame
                        viewModel.resizePlacement(placement.id, newFrame)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(HANDLE_DOT_DIAMETER_DP.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }

    if (resizingHandle != null) {
        val frame = liveFrame ?: placement.frame
        val unit = viewModel.project.preferredUnit
        val labelText = "${MeasurementFormatter.stringForPoints(frame.width, unit)} × ${MeasurementFormatter.stringForPoints(frame.height, unit)}"
        val labelCenter = rotatedScreenPosition(PointD(0.0, -placement.size.height / 2 - 24.0 / scale), placement, scale)
        DimensionLabel(labelText, labelCenter)
    }

    // Rotate handle
    run {
        val gapPagePoints = rotateGapPx / scale
        val localOffset = PointD(0.0, -(placement.size.height / 2) - gapPagePoints)
        val handleCenter = rotatedScreenPosition(localOffset, placement, scale)
        val edgeMidpoint = rotatedScreenPosition(PointD(0.0, -placement.size.height / 2), placement, scale)

        ConnectorLine(edgeMidpoint, handleCenter)

        Box(
            modifier = Modifier
                .offset { IntOffset((handleCenter.x - touchTargetHalfPx).toInt(), (handleCenter.y - touchTargetHalfPx).toInt()) }
                .size(HANDLE_TOUCH_TARGET_DP.dp)
                .pointerInput(placement.id, scale) {
                    detectDragGestures(
                        onDragStart = {
                            rotateCurrentScreen = handleCenter
                            isRotating = true
                            viewModel.beginBatch()
                        },
                        onDragEnd = {
                            rotateCurrentScreen = null
                            isRotating = false
                            liveRotationDegrees = null
                            viewModel.endBatch()
                        },
                        onDragCancel = {
                            rotateCurrentScreen = null
                            isRotating = false
                            liveRotationDegrees = null
                            viewModel.endBatch()
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val current = (rotateCurrentScreen ?: handleCenter) + dragAmount
                        rotateCurrentScreen = current
                        val currentPagePoint = PointD((current.x / scale).toDouble(), (current.y / scale).toDouble())
                        if (currentPagePoint == placement.center) return@detectDragGestures
                        val touchAngle = RotationMath.angleDegrees(placement.center, currentPagePoint)
                        val newRotation = touchAngle + 90
                        liveRotationDegrees = newRotation
                        viewModel.setPlacementRotation(placement.id, newRotation)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(HANDLE_DOT_DIAMETER_DP.dp + 6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Autorenew, contentDescription = "Rotate", tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }

    if (isRotating) {
        val degrees = (liveRotationDegrees ?: placement.placementRotationDegrees).let { if (it < 0) it + 360 else it }.mod(360.0)
        val gapPagePoints = rotateGapPx / scale
        val handleCenter = rotatedScreenPosition(PointD(0.0, -(placement.size.height / 2) - gapPagePoints), placement, scale)
        DimensionLabel("${degrees.toInt()}°", Offset(handleCenter.x, handleCenter.y - labelGapPx))
    }
}

@Composable
private fun DimensionLabel(text: String, center: Offset) {
    Box(
        modifier = Modifier
            .offset { IntOffset(center.x.toInt(), center.y.toInt()) }
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ConnectorLine(from: Offset, to: Offset) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawLine(
            color = Color(0xFF0065E9).copy(alpha = 0.55f),
            start = from,
            end = to,
            strokeWidth = 1.5f
        )
    }
}
