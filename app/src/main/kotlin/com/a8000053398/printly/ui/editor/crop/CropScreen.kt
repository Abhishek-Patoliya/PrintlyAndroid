@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.a8000053398.printly.ui.editor.crop

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a8000053398.printly.core.SizeD
import com.a8000053398.printly.model.BorderStyle
import com.a8000053398.printly.model.OverlayPosition
import com.a8000053398.printly.model.PhotoBackgroundPreset
import com.a8000053398.printly.model.PhotoBackgroundReplacement
import com.a8000053398.printly.model.PhotoFitMode
import com.a8000053398.printly.model.PhotoItem
import com.a8000053398.printly.model.PhotoTransform
import com.a8000053398.printly.model.RGBColor
import com.a8000053398.printly.model.TextOverlay
import com.a8000053398.printly.service.CodeGeneratorService
import com.a8000053398.printly.service.photo.PhotoCropService
import com.a8000053398.printly.ui.components.PrimaryButton
import com.a8000053398.printly.ui.components.SecondaryButton
import com.a8000053398.printly.ui.theme.Metrics
import com.a8000053398.printly.util.PrintlyConstants
import com.a8000053398.printly.viewmodel.ProjectEditorViewModel
import kotlinx.coroutines.launch

private enum class PhotoEditTab(val label: String) { CROP("Crop"), BACKGROUND("Background"), OVERLAY("Text"), ADJUST("Adjust") }

/** Full-screen photo editor for one photo: crop (pan/zoom within a fixed
 * aspect-ratio mask), flip, fit/fill, background swap, text/border overlay,
 * and brightness/contrast. Kotlin/Compose port of iOS's `CropSheet`. */
@Composable
fun CropScreen(viewModel: ProjectEditorViewModel, photo: PhotoItem, onDone: () -> Unit) {
    val aspectRatio = PhotoCropService.targetAspectRatio(viewModel.project.photoSize)
    val availableTabs = remember(photo.isLabel, viewModel.project.photoSize) {
        if (photo.isLabel) listOf(PhotoEditTab.OVERLAY)
        else buildList {
            add(PhotoEditTab.CROP)
            if (viewModel.project.photoSize.preset.isIdentityDocument) add(PhotoEditTab.BACKGROUND)
            add(PhotoEditTab.OVERLAY)
            add(PhotoEditTab.ADJUST)
        }
    }
    var tab by remember { mutableStateOf(if (photo.isLabel) PhotoEditTab.OVERLAY else PhotoEditTab.CROP) }

    var baseImage by remember { mutableStateOf<Bitmap?>(null) }
    var offsetXFraction by remember { mutableStateOf(photo.transform.offsetXFraction) }
    var offsetYFraction by remember { mutableStateOf(photo.transform.offsetYFraction) }
    var zoom by remember { mutableStateOf(photo.transform.zoom) }
    var rotationDegrees by remember { mutableStateOf(photo.transform.rotationDegrees) }

    var flipHorizontal by remember { mutableStateOf(photo.flipHorizontal) }
    var flipVertical by remember { mutableStateOf(photo.flipVertical) }
    var fitMode by remember { mutableStateOf(photo.fitMode) }
    var brightness by remember { mutableStateOf(photo.brightness) }
    var contrast by remember { mutableStateOf(photo.contrast) }
    var showNoFaceAlert by remember { mutableStateOf(false) }

    var backgroundSelection by remember { mutableStateOf(photo.backgroundReplacement?.preset) }
    var customBackgroundColor by remember { mutableStateOf(photo.backgroundReplacement?.customColor?.color ?: Color.White) }

    var overlayText by remember { mutableStateOf(photo.textOverlay?.text ?: "") }
    var overlayPosition by remember { mutableStateOf(photo.textOverlay?.position ?: OverlayPosition.BOTTOM_CENTER) }
    var overlayFontSize by remember { mutableStateOf(photo.textOverlay?.fontSizePt ?: 14.0) }
    var overlayColor by remember { mutableStateOf(photo.textOverlay?.color?.color ?: Color.White) }
    var overlayOpacity by remember { mutableStateOf(photo.textOverlay?.opacity ?: 1.0) }
    var overlayHasPill by remember { mutableStateOf(photo.textOverlay?.hasBackgroundPill ?: true) }
    var borderEnabled by remember { mutableStateOf(photo.border != null) }
    var borderWidth by remember { mutableStateOf(photo.border?.widthPt ?: 2.0) }
    var borderColor by remember { mutableStateOf(photo.border?.color?.color ?: Color.Black) }

    var isShowingCodeGenerator by remember { mutableStateOf(false) }
    var codeGeneratorText by remember { mutableStateOf("") }
    var codeGeneratorType by remember { mutableStateOf(CodeGeneratorService.CodeType.QR) }
    var codeGeneratorError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope2()
    val replaceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)?.let { newBitmap ->
                    if (viewModel.replacePhoto(photo.id, newBitmap)) {
                        val updated = viewModel.project.photos.firstOrNull { it.id == photo.id }
                        baseImage = updated?.let { viewModel.baseImageForEditing(it) }
                        zoom = 1.0; rotationDegrees = 0.0; offsetXFraction = 0.0; offsetYFraction = 0.0
                        flipHorizontal = false; flipVertical = false
                    }
                }
            }
        }
    }

    LaunchedEffect(photo.id) { baseImage = viewModel.baseImageForEditing(photo) }

    fun commitAll() {
        if (photo.isLabel) {
            commitOverlay(viewModel, photo.id, overlayText, overlayPosition, overlayFontSize, overlayColor, overlayOpacity, overlayHasPill, borderEnabled, borderWidth, borderColor)
            return
        }
        viewModel.setImageTransform(PhotoTransform(offsetXFraction, offsetYFraction, zoom, rotationDegrees), photo.id)
        viewModel.setFitMode(fitMode, photo.id)
        viewModel.setFlipHorizontal(flipHorizontal, photo.id)
        viewModel.setFlipVertical(flipVertical, photo.id)
        viewModel.setBrightness(brightness, photo.id)
        viewModel.setContrast(contrast, photo.id)
        if (viewModel.project.photoSize.preset.isIdentityDocument) {
            val preset = backgroundSelection
            if (preset == null) viewModel.setBackgroundReplacement(null, photo.id)
            else viewModel.setBackgroundReplacement(PhotoBackgroundReplacement(preset, RGBColor.from(if (preset == PhotoBackgroundPreset.CUSTOM) customBackgroundColor else preset.fixedColor?.color ?: Color.White)), photo.id)
        }
        commitOverlay(viewModel, photo.id, overlayText, overlayPosition, overlayFontSize, overlayColor, overlayOpacity, overlayHasPill, borderEnabled, borderWidth, borderColor)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(modifier = Modifier.fillMaxWidth().padding(Metrics.spacingM.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onDone) { Text("Cancel", color = Color.White) }
            Text(if (photo.isLabel) "Edit Label" else "Edit Photo", color = Color.White, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { commitAll(); onDone() }) { Text("Done", color = Color.White, fontWeight = FontWeight.Bold) }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val image = baseImage
            if (image != null) {
                val effectiveScale = if (fitMode == PhotoFitMode.FIT) 1f else {
                    val coverMultiplier = com.a8000053398.printly.service.layout.ImageTransformMath.coverMultiplier(aspectRatio, rotationDegrees * Math.PI / 180)
                    (coverMultiplier * kotlin.math.max(1.0, zoom)).toFloat()
                }
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = null,
                    contentScale = if (fitMode == PhotoFitMode.FIT) ContentScale.Fit else ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize(0.85f)
                        .graphicsLayer {
                            scaleX = if (flipHorizontal) -effectiveScale else effectiveScale
                            scaleY = if (flipVertical) -effectiveScale else effectiveScale
                            rotationZ = if (fitMode == PhotoFitMode.FIT) 0f else rotationDegrees.toFloat()
                            translationX = if (fitMode == PhotoFitMode.FIT) 0f else (offsetXFraction * size.width).toFloat()
                            translationY = if (fitMode == PhotoFitMode.FIT) 0f else (offsetYFraction * size.height).toFloat()
                        }
                        .pointerInput(tab, fitMode) {
                            if (tab == PhotoEditTab.CROP && fitMode == PhotoFitMode.FILL) {
                                detectTransformGestures { _, pan, gestureZoom, gestureRotation ->
                                    zoom = (zoom * gestureZoom).coerceIn(1.0, PrintlyConstants.MAX_IMAGE_ZOOM)
                                    rotationDegrees += gestureRotation.toDouble()
                                    val maskWidth = size.width.toFloat()
                                    val maskHeight = size.height.toFloat()
                                    offsetXFraction += pan.x / maskWidth
                                    offsetYFraction += pan.y / maskHeight
                                    val clamped = com.a8000053398.printly.service.layout.ImageTransformMath.clampedOffsetFraction(
                                        com.a8000053398.printly.service.layout.ImageTransformMath.Offset(offsetXFraction, offsetYFraction),
                                        aspectRatio, zoom, rotationDegrees * Math.PI / 180
                                    )
                                    offsetXFraction = clamped.width
                                    offsetYFraction = clamped.height
                                }
                            }
                        }
                )
            } else {
                androidx.compose.material3.CircularProgressIndicator(color = Color.White)
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = Metrics.spacingM.dp)) {
            if (availableTabs.size > 1) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = Metrics.spacingM.dp)) {
                    availableTabs.forEachIndexed { index, t ->
                        SegmentedButton(selected = tab == t, onClick = { tab = t }, shape = SegmentedButtonDefaults.itemShape(index, availableTabs.size)) { Text(t.label) }
                    }
                }
            }

            when (tab) {
                PhotoEditTab.CROP -> CropControls(
                    fitMode = fitMode, onFitModeChange = { fitMode = it },
                    onRotate90 = { rotationDegrees += 90.0 },
                    onReset = { zoom = 1.0; rotationDegrees = 0.0; offsetXFraction = 0.0; offsetYFraction = 0.0 },
                    isIdentityDocument = viewModel.project.photoSize.preset.isIdentityDocument,
                    onAutoCenterFace = {
                        scope.launch {
                            val success = viewModel.autoCenterFaceCrop(photo.id)
                            if (!success) showNoFaceAlert = true
                            else {
                                val updated = viewModel.project.photos.firstOrNull { it.id == photo.id }
                                baseImage = updated?.let { viewModel.baseImageForEditing(it) }
                                zoom = 1.0; rotationDegrees = 0.0; offsetXFraction = 0.0; offsetYFraction = 0.0
                            }
                        }
                    },
                    onReplace = { replaceLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                )
                PhotoEditTab.BACKGROUND -> BackgroundControls(backgroundSelection, customBackgroundColor, onSelect = { backgroundSelection = it }, onColorChange = { customBackgroundColor = it })
                PhotoEditTab.OVERLAY -> OverlayControls(
                    isLabel = photo.isLabel, text = overlayText, onTextChange = { overlayText = it },
                    position = overlayPosition, onPositionChange = { overlayPosition = it },
                    fontSize = overlayFontSize, onFontSizeChange = { overlayFontSize = it },
                    color = overlayColor, onColorChange = { overlayColor = it },
                    opacity = overlayOpacity, onOpacityChange = { overlayOpacity = it },
                    hasPill = overlayHasPill, onHasPillChange = { overlayHasPill = it },
                    borderEnabled = borderEnabled, onBorderEnabledChange = { borderEnabled = it },
                    borderWidth = borderWidth, onBorderWidthChange = { borderWidth = it },
                    borderColor = borderColor, onBorderColorChange = { borderColor = it },
                    onInsertCode = { codeGeneratorText = ""; codeGeneratorError = null; isShowingCodeGenerator = true }
                )
                PhotoEditTab.ADJUST -> AdjustControls(
                    flipHorizontal, { flipHorizontal = it }, flipVertical, { flipVertical = it },
                    brightness, { brightness = it }, contrast, { contrast = it },
                    onResetAll = { flipHorizontal = false; flipVertical = false; fitMode = PhotoFitMode.FILL; brightness = 0.0; contrast = 1.0; zoom = 1.0; rotationDegrees = 0.0; offsetXFraction = 0.0; offsetYFraction = 0.0 }
                )
            }
        }
    }

    if (showNoFaceAlert) {
        AlertDialog(
            onDismissRequest = { showNoFaceAlert = false },
            title = { Text("No Face Detected") },
            text = { Text("Couldn't find a face in this photo. Position the crop manually instead.") },
            confirmButton = { TextButton(onClick = { showNoFaceAlert = false }) { Text("OK") } }
        )
    }

    if (isShowingCodeGenerator) {
        AlertDialog(
            onDismissRequest = { isShowingCodeGenerator = false },
            title = { Text("Insert Code") },
            text = {
                Column {
                    TextField(value = codeGeneratorText, onValueChange = { codeGeneratorText = it }, modifier = Modifier.fillMaxWidth())
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = Metrics.spacingS.dp)) {
                        CodeGeneratorService.CodeType.entries.forEachIndexed { index, type ->
                            SegmentedButton(selected = codeGeneratorType == type, onClick = { codeGeneratorType = type }, shape = SegmentedButtonDefaults.itemShape(index, CodeGeneratorService.CodeType.entries.size)) { Text(type.displayName) }
                        }
                    }
                    codeGeneratorError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val physical = viewModel.project.photoSize.physicalSize
                    val aspect = if (physical.height > 0) physical.width / physical.height else 1.0
                    val width = 600.0
                    val height = kotlin.math.max(1.0, width / kotlin.math.max(aspect, 0.01))
                    val result = runCatching { CodeGeneratorService.generate(codeGeneratorText, codeGeneratorType, SizeD(width, height)) }
                    result.onSuccess {
                        viewModel.replacePhoto(photo.id, it)
                        isShowingCodeGenerator = false
                        onDone()
                    }.onFailure { codeGeneratorError = "Couldn't generate that code. Try shorter text." }
                }, enabled = codeGeneratorText.isNotBlank()) { Text("Insert") }
            },
            dismissButton = { TextButton(onClick = { isShowingCodeGenerator = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun rememberCoroutineScope2() = androidx.compose.runtime.rememberCoroutineScope()

private fun commitOverlay(
    viewModel: ProjectEditorViewModel, photoID: java.util.UUID, text: String, position: OverlayPosition, fontSize: Double,
    color: Color, opacity: Double, hasPill: Boolean, borderEnabled: Boolean, borderWidth: Double, borderColor: Color
) {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) viewModel.setTextOverlay(null, photoID)
    else viewModel.setTextOverlay(TextOverlay(text, position, fontSize, RGBColor.from(color), opacity, hasPill), photoID)

    if (borderEnabled) viewModel.setBorder(BorderStyle(borderWidth, RGBColor.from(borderColor)), photoID)
    else viewModel.setBorder(null, photoID)
}

@Composable
private fun CropControls(fitMode: PhotoFitMode, onFitModeChange: (PhotoFitMode) -> Unit, onRotate90: () -> Unit, onReset: () -> Unit, isIdentityDocument: Boolean, onAutoCenterFace: () -> Unit, onReplace: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Metrics.spacingM.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PhotoFitMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(selected = fitMode == mode, onClick = { onFitModeChange(mode) }, shape = SegmentedButtonDefaults.itemShape(index, PhotoFitMode.entries.size)) { Text(mode.displayName) }
            }
        }
        Text(
            if (fitMode == PhotoFitMode.FIT) "Whole photo shown, padded to fit" else "Drag to move · Pinch to zoom · Twist to rotate",
            color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp
        )
        if (fitMode == PhotoFitMode.FILL) {
            Row(horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
                SecondaryButton("Rotate 90°", onClick = onRotate90, modifier = Modifier.weight(1f))
                SecondaryButton("Reset Image", onClick = onReset, modifier = Modifier.weight(1f))
            }
        }
        if (isIdentityDocument) SecondaryButton("Auto-Center Face", onClick = onAutoCenterFace)
        SecondaryButton("Replace Photo", onClick = onReplace)
    }
}

@Composable
private fun BackgroundControls(selection: PhotoBackgroundPreset?, customColor: Color, onSelect: (PhotoBackgroundPreset?) -> Unit, onColorChange: (Color) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Metrics.spacingM.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
            listOf<PhotoBackgroundPreset?>(null, *PhotoBackgroundPreset.entries.toTypedArray()).forEach { preset ->
                val swatchColor = preset?.fixedColor?.color ?: if (preset == null) Color.Transparent else customColor
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (preset == null) Color.DarkGray else swatchColor)
                            .then(if (selection == preset) Modifier.border(2.dp, Color.White, androidx.compose.foundation.shape.CircleShape) else Modifier)
                            .clickableNoIndication { onSelect(preset) }
                    )
                    Text(preset?.displayName ?: "None", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                }
            }
        }
        if (selection == PhotoBackgroundPreset.CUSTOM) {
            Text("Custom color editing uses the system color picker on double-tap (not shown here); defaults to white.", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
        Text("Background removal runs entirely on-device. Your original photo is never changed.", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

@Composable
private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier {
    val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

@Composable
private fun OverlayControls(
    isLabel: Boolean, text: String, onTextChange: (String) -> Unit,
    position: OverlayPosition, onPositionChange: (OverlayPosition) -> Unit,
    fontSize: Double, onFontSizeChange: (Double) -> Unit,
    color: Color, onColorChange: (Color) -> Unit,
    opacity: Double, onOpacityChange: (Double) -> Unit,
    hasPill: Boolean, onHasPillChange: (Boolean) -> Unit,
    borderEnabled: Boolean, onBorderEnabledChange: (Boolean) -> Unit,
    borderWidth: Double, onBorderWidthChange: (Double) -> Unit,
    borderColor: Color, onBorderColorChange: (Color) -> Unit,
    onInsertCode: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Metrics.spacingM.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
        TextField(value = text, onValueChange = onTextChange, placeholder = { Text(if (isLabel) "Label Text" else "Caption, date, or watermark") }, modifier = Modifier.fillMaxWidth())
        if (isLabel) SecondaryButton("Insert QR Code / Barcode", onClick = onInsertCode)

        if (text.isNotBlank()) {
            Text("Position", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            var expanded by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { expanded = true }) { Text(position.displayName, color = Color.White) }
                androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    for (p in OverlayPosition.entries) {
                        androidx.compose.material3.DropdownMenuItem(text = { Text(p.displayName) }, onClick = { onPositionChange(p); expanded = false })
                    }
                }
            }
            Text("Font Size: ${fontSize.toInt()}pt", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Slider(value = fontSize.toFloat(), onValueChange = { onFontSizeChange(it.toDouble()) }, valueRange = 8f..72f)
            Text("Opacity (lower = watermark)", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Slider(value = opacity.toFloat(), onValueChange = { onOpacityChange(it.toDouble()) }, valueRange = 0.1f..1f)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Background Pill", color = Color.White)
                Switch(checked = hasPill, onCheckedChange = onHasPillChange)
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Border", color = Color.White)
            Switch(checked = borderEnabled, onCheckedChange = onBorderEnabledChange)
        }
        if (borderEnabled) {
            Text("Border Width: %.1fpt".format(borderWidth), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Slider(value = borderWidth.toFloat(), onValueChange = { onBorderWidthChange(it.toDouble()) }, valueRange = 0.5f..12f)
        }
    }
}

@Composable
private fun AdjustControls(
    flipHorizontal: Boolean, onFlipHorizontalChange: (Boolean) -> Unit,
    flipVertical: Boolean, onFlipVerticalChange: (Boolean) -> Unit,
    brightness: Double, onBrightnessChange: (Double) -> Unit,
    contrast: Double, onContrastChange: (Double) -> Unit,
    onResetAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Metrics.spacingM.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
            SecondaryButton(if (flipHorizontal) "Flip Horizontal ✓" else "Flip Horizontal", onClick = { onFlipHorizontalChange(!flipHorizontal) }, modifier = Modifier.weight(1f))
            SecondaryButton(if (flipVertical) "Flip Vertical ✓" else "Flip Vertical", onClick = { onFlipVerticalChange(!flipVertical) }, modifier = Modifier.weight(1f))
        }
        Text("Brightness", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        Slider(value = brightness.toFloat(), onValueChange = { onBrightnessChange(it.toDouble()) }, valueRange = -0.5f..0.5f)
        Text("Contrast", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        Slider(value = contrast.toFloat(), onValueChange = { onContrastChange(it.toDouble()) }, valueRange = 0.5f..1.5f)
        Button(onClick = onResetAll, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Reset All Adjustments") }
    }
}
