package com.a8000053398.printly.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a8000053398.printly.core.PointD
import com.a8000053398.printly.core.RectD
import com.a8000053398.printly.core.SizeD
import com.a8000053398.printly.model.BorderStyle
import com.a8000053398.printly.model.ManualPlacementOverride
import com.a8000053398.printly.model.PageBackground
import com.a8000053398.printly.model.PageMargins
import com.a8000053398.printly.model.PageSize
import com.a8000053398.printly.model.PageSpacing
import com.a8000053398.printly.model.PhotoBackgroundReplacement
import com.a8000053398.printly.model.PhotoFitMode
import com.a8000053398.printly.model.PhotoItem
import com.a8000053398.printly.model.PhotoSize
import com.a8000053398.printly.model.PhotoTransform
import com.a8000053398.printly.model.PlacedPhoto
import com.a8000053398.printly.model.PrintProject
import com.a8000053398.printly.model.PrintTemplate
import com.a8000053398.printly.model.TextOverlay
import com.a8000053398.printly.service.PDFImportService
import com.a8000053398.printly.service.PhysicalMeasurementService
import com.a8000053398.printly.service.TemplateService
import com.a8000053398.printly.service.layout.PageLayoutEngine
import com.a8000053398.printly.service.layout.ResizeGeometryService
import com.a8000053398.printly.service.pdf.ImageExportService
import com.a8000053398.printly.service.pdf.PDFExportService
import com.a8000053398.printly.service.persistence.ExportHistoryStore
import com.a8000053398.printly.service.persistence.ProjectPersistenceService
import com.a8000053398.printly.service.photo.BackgroundRemovalService
import com.a8000053398.printly.service.photo.FaceDetectionService
import com.a8000053398.printly.service.photo.PhotoCropService
import com.a8000053398.printly.service.photo.PhotoImportService
import com.a8000053398.printly.model.ExportedFileFormat
import com.a8000053398.printly.util.PrintlyConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Drives the page editor: owns the [PrintProject], recomputes layout via
 * [PageLayoutEngine] whenever inputs change, and coordinates cropping,
 * persistence, and PDF export. Direct Kotlin port of the iOS
 * `ProjectEditorViewModel`. */
class ProjectEditorViewModel(
    initialProject: PrintProject = PrintProject(),
    private val persistence: ProjectPersistenceService = ProjectPersistenceService.shared
) : ViewModel() {

    var project by mutableStateOf(initialProject)
        private set

    var pages by mutableStateOf<List<LayoutResultUi>>(emptyList())
        private set

    var currentPageIndex by mutableStateOf(0)
        private set
    var selectedPlacementID by mutableStateOf<UUID?>(null)
    var isImportingPhotos by mutableStateOf(false)
        private set
    var isExporting by mutableStateOf(false)
        private set
    var exportedPDFFile by mutableStateOf<File?>(null)
        private set
    var exportedImageFiles by mutableStateOf<List<File>>(emptyList())
        private set
    var errorMessage by mutableStateOf<String?>(null)

    var isSelectingMultiplePlacements by mutableStateOf(false)
        private set
    var multiSelectedPlacementIDs by mutableStateOf<Set<UUID>>(emptySet())

    private val imageCache = HashMap<String, Bitmap>()

    // MARK: - Undo / Redo

    private val undoStack = ArrayDeque<PrintProject>()
    private val redoStack = ArrayDeque<PrintProject>()
    private val maxHistoryDepth = 50

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    private var suppressionDepth = 0
    private var autosaveJob: Job? = null

    init {
        recomputeLayout()
    }

    private fun mutateProject(block: (PrintProject) -> PrintProject) {
        val old = project
        val updated = block(old)
        if (suppressionDepth == 0 && !projectsEquivalentForUndo(old, updated)) {
            pushUndo(old)
        }
        project = updated
    }

    fun beginBatch() {
        if (suppressionDepth == 0) pushUndo(project)
        suppressionDepth += 1
    }

    fun endBatch() {
        suppressionDepth = maxOf(0, suppressionDepth - 1)
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        val current = project
        suppressionDepth += 1
        project = previous
        recomputeLayout()
        suppressionDepth = maxOf(0, suppressionDepth - 1)
        redoStack.addLast(current)
        pruneSelectionIfDangling()
        updateUndoRedoFlags()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        val current = project
        suppressionDepth += 1
        project = next
        recomputeLayout()
        suppressionDepth = maxOf(0, suppressionDepth - 1)
        undoStack.addLast(current)
        pruneSelectionIfDangling()
        updateUndoRedoFlags()
    }

    private fun pushUndo(snapshot: PrintProject) {
        undoStack.addLast(snapshot)
        while (undoStack.size > maxHistoryDepth) undoStack.removeFirst()
        redoStack.clear()
        updateUndoRedoFlags()
    }

    private fun updateUndoRedoFlags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    private fun projectsEquivalentForUndo(a: PrintProject, b: PrintProject): Boolean =
        a.copy(modifiedAt = b.modifiedAt) == b

    private fun pruneSelectionIfDangling() {
        val id = selectedPlacementID ?: return
        if (layout.placements.none { it.id == id }) selectedPlacementID = null
    }

    // MARK: - Layout

    val layout: LayoutResultUi
        get() = pages.getOrNull(currentPageIndex) ?: pages.firstOrNull() ?: LayoutResultUi.empty(project)

    val totalPages: Int get() = maxOf(1, pages.size)

    val resolvedOrientation: com.a8000053398.printly.model.PageOrientation
        get() {
            val size = pages.firstOrNull()?.pageSize ?: return com.a8000053398.printly.model.PageOrientation.PORTRAIT
            return if (size.width > size.height) com.a8000053398.printly.model.PageOrientation.LANDSCAPE else com.a8000053398.printly.model.PageOrientation.PORTRAIT
        }

    fun resolvedPageSize(): SizeD {
        if (project.pageSize.orientation != com.a8000053398.printly.model.PageOrientation.AUTO) return project.pageSize.pointSize

        val portraitSize = project.pageSize.physicalSize(com.a8000053398.printly.model.PageOrientation.PORTRAIT).let { PhysicalMeasurementService.points(it) }
        val landscapeSize = project.pageSize.physicalSize(com.a8000053398.printly.model.PageOrientation.LANDSCAPE).let { PhysicalMeasurementService.points(it) }

        val portraitCapacity = PageLayoutEngine.layout(portraitSize, project.margins, project.photoSize.pointSize, project.spacing, emptyList(), project.allowRotationToFit).maxCapacity
        val landscapeCapacity = PageLayoutEngine.layout(landscapeSize, project.margins, project.photoSize.pointSize, project.spacing, emptyList(), project.allowRotationToFit).maxCapacity

        return if (landscapeCapacity > portraitCapacity) landscapeSize else portraitSize
    }

    fun recomputeLayout() {
        var result = PageLayoutEngine.paginate(
            pageSize = resolvedPageSize(),
            margins = project.margins,
            photoSize = project.photoSize.pointSize,
            spacing = project.spacing,
            photoSequence = project.expandedPhotoSequence,
            allowRotationToFit = project.allowRotationToFit,
            fillPageEdgeToEdge = project.fillPageEdgeToEdge
        )

        result = result.map { layoutResult ->
            val updatedPlacements = layoutResult.placements.map { placement ->
                val override = project.manualPlacementOverrides[placement.id.toString()]
                if (override != null) {
                    placement.copy(
                        origin = override.origin,
                        size = override.size ?: placement.size,
                        placementRotationDegrees = override.rotationDegrees,
                        isManuallyPositioned = true
                    )
                } else placement
            }
            layoutResult.copy(placements = updatedPlacements)
        }

        pages = result.map { LayoutResultUi.from(it) }
        if (currentPageIndex !in pages.indices) currentPageIndex = maxOf(0, pages.size - 1)
        project = project.copy(modifiedAt = System.currentTimeMillis())
        scheduleAutosave()
    }

    fun resetManualArrangement() {
        mutateProject { it.copy(manualPlacementOverrides = emptyMap()) }
        recomputeLayout()
    }

    val layoutError: String?
        get() {
            val first = pages.firstOrNull() ?: return null
            if (first.maxCapacity != 0 || project.totalRequestedCopies <= 0) return null
            return "This photo doesn't fit on the page. Try a smaller photo size, a larger page size, or smaller margins."
        }

    val totalPlacedCount: Int get() = pages.sumOf { it.placements.size }

    // MARK: - Page navigation

    fun goToPage(index: Int) {
        if (index !in pages.indices) return
        currentPageIndex = index
        selectedPlacementID = null
    }

    fun goToNextPage() { if (currentPageIndex + 1 < pages.size) goToPage(currentPageIndex + 1) }
    fun goToPreviousPage() { if (currentPageIndex > 0) goToPage(currentPageIndex - 1) }

    // MARK: - Photos

    fun importPhotos(context: android.content.Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        isImportingPhotos = true
        viewModelScope.launch {
            val images = withContext(Dispatchers.IO) { PhotoImportService.loadImages(context, uris) }
            for (image in images) addPhoto(image)
            recomputeLayout()
            isImportingPhotos = false
        }
    }

    fun importPDF(context: android.content.Context, uri: Uri, onError: (String?) -> Unit) {
        isImportingPhotos = true
        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { PDFImportService.importPages(context, uri) } }
            isImportingPhotos = false
            result.onSuccess { images ->
                if (images.isEmpty()) {
                    onError("This PDF has no pages that could be imported.")
                } else {
                    for (image in images) addPhoto(image)
                    recomputeLayout()
                    onError(null)
                }
            }.onFailure { onError("Couldn't import PDF: ${it.message}") }
        }
    }

    fun addPhoto(bitmap: Bitmap) {
        val fileName = persistence.saveImage(bitmap)
        imageCache[fileName] = bitmap
        val nextIndex = (project.photos.maxOfOrNull { it.sortIndex } ?: -1) + 1
        val cropRect = PhotoCropService.centeredCropRect(
            imageSize = SizeD(bitmap.width.toDouble(), bitmap.height.toDouble()),
            aspectRatio = PhotoCropService.targetAspectRatio(project.photoSize)
        )
        var photo = PhotoItem(originalFileName = fileName, cropRect = cropRect, sortIndex = nextIndex)
        val pendingCopies = project.pendingCopiesForFirstPhoto
        if (pendingCopies != null) {
            photo = photo.copy(copies = pendingCopies)
        }
        mutateProject {
            it.copy(
                photos = it.photos + photo,
                pendingCopiesForFirstPhoto = if (pendingCopies != null) null else it.pendingCopiesForFirstPhoto
            )
        }
    }

    fun deletePhoto(photo: PhotoItem) {
        mutateProject { it.copy(photos = it.photos.filterNot { p -> p.id == photo.id }) }
        imageCache.remove(photo.activeFileName)
        persistence.deleteImage(photo.originalFileName)
        photo.editedFileName?.let { persistence.deleteImage(it) }
        recomputeLayout()
    }

    fun duplicatePhoto(photo: PhotoItem) {
        val index = project.photos.indexOfFirst { it.id == photo.id }
        if (index < 0) return
        val duplicated = photo.copy(id = UUID.randomUUID(), copies = 1, sortIndex = photo.sortIndex + 1)
        mutateProject { p ->
            val shifted = p.photos.mapIndexed { i, ph -> if (i > index) ph.copy(sortIndex = ph.sortIndex + 1) else ph }
            val mutable = shifted.toMutableList()
            mutable.add(index + 1, duplicated)
            p.copy(photos = mutable)
        }
        recomputeLayout()
    }

    fun movePhotos(from: Int, to: Int) {
        val sorted = project.photos.sortedBy { it.sortIndex }.toMutableList()
        val item = sorted.removeAt(from)
        sorted.add(to, item)
        val reindexed = sorted.mapIndexed { index, photo -> photo.copy(sortIndex = index) }
        mutateProject { it.copy(photos = reindexed) }
        recomputeLayout()
    }

    fun setCopies(copies: Int, photo: PhotoItem) {
        mutateProject { p -> p.copy(photos = p.photos.map { if (it.id == photo.id) it.copy(copies = maxOf(0, copies)) else it }) }
        recomputeLayout()
    }

    fun image(photoID: UUID): Bitmap? {
        val photo = project.photos.firstOrNull { it.id == photoID } ?: return null
        return resolvedImage(photo)
    }

    fun resolvedImage(photo: PhotoItem): Bitmap? {
        val cacheKey = "resolved-${photo.id}-${photo.cropRect}-${photo.rotationDegrees}-${photo.flipHorizontal}-${photo.flipVertical}-${photo.brightness}-${photo.contrast}-${photo.backgroundReplacement}-${photo.fitMode}-${photo.textOverlay}-${photo.border}-${photo.transform}"
        imageCache[cacheKey]?.let { return it }
        val original = loadOriginalImage(photo) ?: return null

        val resolved = runCatching {
            PhotoCropService.renderCroppedImage(
                bitmap = original, cropRect = photo.cropRect, rotationDegrees = photo.rotationDegrees,
                flipHorizontal = photo.flipHorizontal, flipVertical = photo.flipVertical,
                brightness = photo.brightness, contrast = photo.contrast,
                backgroundReplacement = photo.backgroundReplacement, transform = photo.transform,
                fitMode = photo.fitMode, border = photo.border, textOverlay = photo.textOverlay,
                targetPointSize = project.photoSize.pointSize
            )
        }.getOrNull() ?: original
        imageCache[cacheKey] = resolved
        return resolved
    }

    fun baseImageForEditing(photo: PhotoItem): Bitmap? {
        val original = loadOriginalImage(photo) ?: return null
        return PhotoCropService.renderBaseImage(
            original, photo.cropRect, photo.rotationDegrees, photo.flipHorizontal, photo.flipVertical, photo.backgroundReplacement
        )
    }

    @Suppress("unused")
    fun addBlankLabel(): UUID? {
        val physical = project.photoSize.physicalSize
        val aspect = if (physical.height > 0) physical.width / physical.height else 1.0
        val width = 600.0
        val height = maxOf(1.0, width / maxOf(aspect, 0.01))
        val blank = PhotoCropService.blankImage(SizeD(width, height))

        val fileName = persistence.saveImage(blank)
        imageCache[fileName] = blank
        val nextIndex = (project.photos.maxOfOrNull { it.sortIndex } ?: -1) + 1
        var photo = PhotoItem(originalFileName = fileName, sortIndex = nextIndex, isLabel = true, textOverlay = TextOverlay.default)
        val pendingCopies = project.pendingCopiesForFirstPhoto
        if (pendingCopies != null) photo = photo.copy(copies = pendingCopies)
        mutateProject {
            it.copy(photos = it.photos + photo, pendingCopiesForFirstPhoto = if (pendingCopies != null) null else it.pendingCopiesForFirstPhoto)
        }
        recomputeLayout()
        return photo.id
    }

    fun replacePhoto(photoID: UUID, bitmap: Bitmap): Boolean {
        val index = project.photos.indexOfFirst { it.id == photoID }
        if (index < 0) return false
        val fileName = persistence.saveImage(bitmap)
        val old = project.photos[index]

        val cropRect = PhotoCropService.centeredCropRect(SizeD(bitmap.width.toDouble(), bitmap.height.toDouble()), PhotoCropService.targetAspectRatio(project.photoSize))
        mutateProject { p ->
            p.copy(photos = p.photos.mapIndexed { i, ph ->
                if (i == index) ph.copy(
                    originalFileName = fileName, editedFileName = null, isLabel = false,
                    cropRect = cropRect, rotationDegrees = 0.0, flipHorizontal = false, flipVertical = false,
                    transform = PhotoTransform.identity
                ) else ph
            })
        }

        imageCache.remove(old.originalFileName)
        old.editedFileName?.let { imageCache.remove(it) }
        persistence.deleteImage(old.originalFileName)
        old.editedFileName?.let { persistence.deleteImage(it) }
        imageCache[fileName] = bitmap
        invalidateResolvedCache(old)
        recomputeLayout()
        return true
    }

    private fun loadOriginalImage(photo: PhotoItem): Bitmap? {
        imageCache[photo.originalFileName]?.let { return it }
        val loaded = persistence.loadImage(photo.originalFileName) ?: return null
        imageCache[photo.originalFileName] = loaded
        return loaded
    }

    fun rotatePhoto(photoID: UUID, degrees: Double = 90.0) {
        val index = project.photos.indexOfFirst { it.id == photoID }
        if (index < 0) return
        mutateProject { p -> p.copy(photos = p.photos.mapIndexed { i, ph -> if (i == index) ph.copy(rotationDegrees = (ph.rotationDegrees + degrees).mod(360.0)) else ph }) }
        invalidateResolvedCache(project.photos[index])
        recomputeLayout()
    }

    fun setFlipHorizontal(flipped: Boolean, photoID: UUID) = updatePhoto(photoID) { it.copy(flipHorizontal = flipped) }
    fun setFlipVertical(flipped: Boolean, photoID: UUID) = updatePhoto(photoID) { it.copy(flipVertical = flipped) }
    fun setBrightness(value: Double, photoID: UUID) = updatePhoto(photoID) { it.copy(brightness = value) }
    fun setContrast(value: Double, photoID: UUID) = updatePhoto(photoID) { it.copy(contrast = value) }
    fun setFitMode(mode: PhotoFitMode, photoID: UUID) = updatePhoto(photoID) {
        if (mode == PhotoFitMode.FIT) it.copy(fitMode = mode, cropRect = RectD.unitSquare) else it.copy(fitMode = mode)
    }
    fun setTextOverlay(overlay: TextOverlay?, photoID: UUID) = updatePhoto(photoID) { it.copy(textOverlay = overlay) }
    fun setBorder(border: BorderStyle?, photoID: UUID) = updatePhoto(photoID) { it.copy(border = border) }
    fun setImageTransform(transform: PhotoTransform, photoID: UUID) = updatePhoto(photoID) { it.copy(transform = transform) }
    fun setBackgroundReplacement(replacement: PhotoBackgroundReplacement?, photoID: UUID) = updatePhoto(photoID) { it.copy(backgroundReplacement = replacement) }

    private fun updatePhoto(photoID: UUID, transform: (PhotoItem) -> PhotoItem) {
        val index = project.photos.indexOfFirst { it.id == photoID }
        if (index < 0) return
        mutateProject { p -> p.copy(photos = p.photos.mapIndexed { i, ph -> if (i == index) transform(ph) else ph }) }
        invalidateResolvedCache(project.photos[index])
        recomputeLayout()
    }

    fun adjustImageTransform(photoID: UUID, zoomFactor: Double? = null, rotationDelta: Double? = null) {
        val index = project.photos.indexOfFirst { it.id == photoID }
        if (index < 0) return
        var transform = project.photos[index].transform
        if (zoomFactor != null && zoomFactor.isFinite() && zoomFactor > 0) {
            transform = transform.copy(zoom = (transform.zoom * zoomFactor).coerceIn(1.0, PrintlyConstants.MAX_IMAGE_ZOOM))
        }
        if (rotationDelta != null && rotationDelta.isFinite()) {
            transform = transform.copy(rotationDegrees = transform.rotationDegrees + rotationDelta)
        }
        val finalTransform = transform
        updatePhoto(photoID) { it.copy(transform = finalTransform) }
    }

    fun resetAdjustments(photoID: UUID) {
        val index = project.photos.indexOfFirst { it.id == photoID }
        if (index < 0) return
        val original = loadOriginalImage(project.photos[index]) ?: return
        val cropRect = PhotoCropService.centeredCropRect(SizeD(original.width.toDouble(), original.height.toDouble()), PhotoCropService.targetAspectRatio(project.photoSize))
        updatePhoto(photoID) {
            it.copy(
                cropRect = cropRect, rotationDegrees = 0.0, flipHorizontal = false, flipVertical = false,
                brightness = 0.0, contrast = 1.0, backgroundReplacement = null, fitMode = PhotoFitMode.FILL, transform = PhotoTransform.identity
            )
        }
    }

    // MARK: - ID photo assist

    suspend fun autoCenterFaceCrop(photoID: UUID): Boolean {
        val index = project.photos.indexOfFirst { it.id == photoID }
        if (index < 0) return false
        val original = loadOriginalImage(project.photos[index]) ?: return false
        val faceBox = withContext(Dispatchers.Default) { FaceDetectionService.detectFaceBoundingBox(original) } ?: return false

        val cropRect = PhotoCropService.faceCenteredCropRect(
            SizeD(original.width.toDouble(), original.height.toDouble()), PhotoCropService.targetAspectRatio(project.photoSize), faceBox
        )
        updatePhoto(photoID) { it.copy(cropRect = cropRect) }
        return true
    }

    fun numberPhotosForContactSheet() {
        val ordered = project.photos.sortedBy { it.sortIndex }
        val numbered = ordered.mapIndexed { position, photo ->
            val overlay = (photo.textOverlay ?: TextOverlay.default).copy(text = "${position + 1}", position = com.a8000053398.printly.model.OverlayPosition.BOTTOM_CENTER)
            photo.id to overlay
        }.toMap()
        mutateProject { p -> p.copy(photos = p.photos.map { if (numbered.containsKey(it.id)) it.copy(textOverlay = numbered[it.id]) else it }) }
        for (photo in project.photos) invalidateResolvedCache(photo)
        recomputeLayout()
    }

    fun clearContactSheetNumbers() {
        mutateProject { p ->
            p.copy(photos = p.photos.map { photo ->
                val overlay = photo.textOverlay
                if (overlay != null && overlay.text.toIntOrNull() != null) photo.copy(textOverlay = null) else photo
            })
        }
        for (photo in project.photos) invalidateResolvedCache(photo)
        recomputeLayout()
    }

    private fun invalidateResolvedCache(photo: PhotoItem) {
        val prefix = "resolved-${photo.id}"
        imageCache.keys.filter { it.startsWith(prefix) }.forEach { imageCache.remove(it) }
    }

    // MARK: - Manual placement (visual editor)

    fun movePlacement(placementID: UUID, origin: PointD) {
        val (pageIndex, index) = locatePlacement(placementID) ?: return
        val page = pages[pageIndex]
        val placement = page.placements[index]
        val size = placement.size
        val clampedX = origin.x.coerceIn(0.0, maxOf(0.0, page.pageSize.width - size.width))
        val clampedY = origin.y.coerceIn(0.0, maxOf(0.0, page.pageSize.height - size.height))
        val clampedOrigin = PointD(clampedX, clampedY)

        updatePages(pageIndex, index) { it.copy(origin = clampedOrigin, isManuallyPositioned = true) }
        val existingSize = project.manualPlacementOverrides[placementID.toString()]?.size
        setOverride(placementID, ManualPlacementOverride(clampedOrigin, existingSize, placement.placementRotationDegrees))
    }

    fun resizePlacement(placementID: UUID, frame: RectD) {
        val (pageIndex, index) = locatePlacement(placementID) ?: return
        val pageSize = pages[pageIndex].pageSize

        val width = frame.width.coerceIn(PrintlyConstants.minResizeDimensionPoints, minOf(PrintlyConstants.maxResizeDimensionPoints, pageSize.width))
        val height = frame.height.coerceIn(PrintlyConstants.minResizeDimensionPoints, minOf(PrintlyConstants.maxResizeDimensionPoints, pageSize.height))
        val clampedX = frame.x.coerceIn(0.0, maxOf(0.0, pageSize.width - width))
        val clampedY = frame.y.coerceIn(0.0, maxOf(0.0, pageSize.height - height))

        val newOrigin = PointD(clampedX, clampedY)
        val newSize = SizeD(width, height)

        updatePages(pageIndex, index) { it.copy(origin = newOrigin, size = newSize, isManuallyPositioned = true) }
        val rotation = pages[pageIndex].placements[index].placementRotationDegrees
        setOverride(placementID, ManualPlacementOverride(newOrigin, newSize, rotation))
    }

    fun rotatePlacement(placementID: UUID, degrees: Double = 90.0) {
        val (pageIndex, index) = locatePlacement(placementID) ?: return
        val newRotation = (pages[pageIndex].placements[index].placementRotationDegrees + degrees).mod(360.0)
        updatePages(pageIndex, index) { it.copy(placementRotationDegrees = newRotation) }
        val origin = pages[pageIndex].placements[index].origin
        val existingSize = project.manualPlacementOverrides[placementID.toString()]?.size
        setOverride(placementID, ManualPlacementOverride(origin, existingSize, newRotation))
    }

    fun setPlacementRotation(placementID: UUID, degrees: Double) {
        val (pageIndex, index) = locatePlacement(placementID) ?: return
        val normalized = degrees.mod(360.0)
        updatePages(pageIndex, index) { it.copy(placementRotationDegrees = normalized, isManuallyPositioned = true) }
        val origin = pages[pageIndex].placements[index].origin
        val existingSize = project.manualPlacementOverrides[placementID.toString()]?.size
        setOverride(placementID, ManualPlacementOverride(origin, existingSize, normalized))
    }

    private fun setOverride(placementID: UUID, override: ManualPlacementOverride) {
        project = project.copy(
            manualPlacementOverrides = project.manualPlacementOverrides + (placementID.toString() to override),
            modifiedAt = System.currentTimeMillis()
        )
        scheduleAutosave()
    }

    private fun updatePages(pageIndex: Int, placementIndex: Int, transform: (PlacedPhoto) -> PlacedPhoto) {
        pages = pages.mapIndexed { pi, page ->
            if (pi != pageIndex) page else page.copy(placements = page.placements.mapIndexed { ppi, p -> if (ppi == placementIndex) transform(p) else p })
        }
    }

    private fun locatePlacement(placementID: UUID): Pair<Int, Int>? {
        pages.getOrNull(currentPageIndex)?.placements?.indexOfFirst { it.id == placementID }?.let { if (it >= 0) return currentPageIndex to it }
        for (pageIndex in pages.indices) {
            val idx = pages[pageIndex].placements.indexOfFirst { it.id == placementID }
            if (idx >= 0) return pageIndex to idx
        }
        return null
    }

    enum class HorizontalAlignment { LEFT, CENTER, RIGHT }
    enum class VerticalAlignment { TOP, MIDDLE, BOTTOM }

    fun centerPlacement(placementID: UUID) = alignPlacement(placementID, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE)

    fun alignPlacement(placementID: UUID, horizontal: HorizontalAlignment?, vertical: VerticalAlignment?) {
        val index = layout.placements.indexOfFirst { it.id == placementID }
        if (index < 0) return
        val size = layout.placements[index].size
        val printable = layout.printableRect
        var origin = layout.placements[index].origin

        origin = when (horizontal) {
            HorizontalAlignment.LEFT -> origin.copy(x = printable.minX)
            HorizontalAlignment.CENTER -> origin.copy(x = printable.midX - size.width / 2)
            HorizontalAlignment.RIGHT -> origin.copy(x = printable.maxX - size.width)
            null -> origin
        }
        origin = when (vertical) {
            VerticalAlignment.TOP -> origin.copy(y = printable.minY)
            VerticalAlignment.MIDDLE -> origin.copy(y = printable.midY - size.height / 2)
            VerticalAlignment.BOTTOM -> origin.copy(y = printable.maxY - size.height)
            null -> origin
        }
        movePlacement(placementID, origin)
    }

    fun duplicatePlacement(placementID: UUID) {
        val placement = layout.placements.firstOrNull { it.id == placementID } ?: return
        val index = project.photos.indexOfFirst { it.id == placement.photoID }
        if (index < 0) return
        mutateProject { p -> p.copy(photos = p.photos.mapIndexed { i, ph -> if (i == index) ph.copy(copies = ph.copies + 1) else ph }) }
        recomputeLayout()
    }

    fun deletePlacement(placementID: UUID) {
        val placement = layout.placements.firstOrNull { it.id == placementID } ?: return
        val index = project.photos.indexOfFirst { it.id == placement.photoID }
        if (index < 0) return

        if (project.photos[index].copies > 1) {
            mutateProject { p -> p.copy(photos = p.photos.mapIndexed { i, ph -> if (i == index) ph.copy(copies = ph.copies - 1) else ph }) }
        } else {
            val photo = project.photos[index]
            mutateProject { p -> p.copy(photos = p.photos.filterNot { it.id == photo.id }) }
            imageCache.remove(photo.activeFileName)
            persistence.deleteImage(photo.originalFileName)
            photo.editedFileName?.let { persistence.deleteImage(it) }
        }

        mutateProject { it.copy(manualPlacementOverrides = it.manualPlacementOverrides - placementID.toString()) }
        if (selectedPlacementID == placementID) selectedPlacementID = null
        recomputeLayout()
    }

    // MARK: - Multi-select

    fun setMultiSelectMode(isSelecting: Boolean) {
        isSelectingMultiplePlacements = isSelecting
        multiSelectedPlacementIDs = emptySet()
        if (isSelecting) selectedPlacementID = null
    }

    fun toggleMultiSelection(placementID: UUID) {
        multiSelectedPlacementIDs = if (multiSelectedPlacementIDs.contains(placementID)) multiSelectedPlacementIDs - placementID else multiSelectedPlacementIDs + placementID
    }

    fun selectAllPlacementsForMultiSelect() {
        multiSelectedPlacementIDs = layout.placements.map { it.id }.toSet()
    }

    fun rotateMultiSelectedPlacements(degrees: Double = 90.0) {
        if (multiSelectedPlacementIDs.isEmpty()) return
        beginBatch()
        for (id in multiSelectedPlacementIDs) rotatePlacement(id, degrees)
        endBatch()
    }

    fun deleteMultiSelectedPlacements() {
        if (multiSelectedPlacementIDs.isEmpty()) return
        val deletionCountByPhotoID = HashMap<UUID, Int>()
        for (id in multiSelectedPlacementIDs) {
            val placement = layout.placements.firstOrNull { it.id == id } ?: continue
            deletionCountByPhotoID[placement.photoID] = (deletionCountByPhotoID[placement.photoID] ?: 0) + 1
        }

        beginBatch()
        for ((photoID, count) in deletionCountByPhotoID) {
            val index = project.photos.indexOfFirst { it.id == photoID }
            if (index < 0) continue
            val remaining = project.photos[index].copies - count
            if (remaining > 0) {
                mutateProject { p -> p.copy(photos = p.photos.mapIndexed { i, ph -> if (i == index) ph.copy(copies = remaining) else ph }) }
            } else {
                val photo = project.photos[index]
                mutateProject { p -> p.copy(photos = p.photos.filterNot { it.id == photo.id }) }
                imageCache.remove(photo.activeFileName)
                persistence.deleteImage(photo.originalFileName)
                photo.editedFileName?.let { persistence.deleteImage(it) }
            }
        }
        if (selectedPlacementID != null && multiSelectedPlacementIDs.contains(selectedPlacementID)) selectedPlacementID = null
        multiSelectedPlacementIDs = emptySet()
        recomputeLayout()
        endBatch()
    }

    // MARK: - Arrange modes

    fun setAllowRotationToFit(allow: Boolean) { mutateProject { it.copy(allowRotationToFit = allow) }; recomputeLayout() }
    fun setFillPageEdgeToEdge(fill: Boolean) { mutateProject { it.copy(fillPageEdgeToEdge = fill) }; recomputeLayout() }
    fun setBackgroundColor(background: PageBackground) { mutateProject { it.copy(backgroundColor = background) }; recomputeLayout() }
    fun setShowCutGuides(show: Boolean) {
        project = project.copy(showCutGuides = show, modifiedAt = System.currentTimeMillis())
        scheduleAutosave()
    }

    // MARK: - Page & photo size

    fun setPageSize(pageSize: PageSize) { mutateProject { it.copy(pageSize = pageSize, manualPlacementOverrides = emptyMap()) }; recomputeLayout() }
    fun setPhotoSize(photoSize: PhotoSize) {
        mutateProject { it.copy(photoSize = photoSize, manualPlacementOverrides = emptyMap()) }
        imageCache.keys.filter { it.startsWith("resolved-") }.forEach { imageCache.remove(it) }
        recomputeLayout()
    }
    fun setMargins(margins: PageMargins) { mutateProject { it.copy(margins = margins) }; recomputeLayout() }
    fun setSpacing(spacing: PageSpacing) { mutateProject { it.copy(spacing = spacing) }; recomputeLayout() }

    fun applyTemplate(template: PrintTemplate) {
        mutateProject { p -> TemplateService.apply(template, p).copy(manualPlacementOverrides = emptyMap()) }
        recomputeLayout()
    }

    fun rename(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        mutateProject { it.copy(name = trimmed) }
        recomputeLayout()
    }

    // MARK: - Persistence

    fun save() {
        runCatching { persistence.save(project) }.onFailure { errorMessage = "Couldn't save project: ${it.message}" }
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            save()
        }
    }

    // MARK: - Quality check

    fun qualityWarning(photo: PhotoItem): String? {
        val original = loadOriginalImage(photo) ?: return null
        val usedPixelSize = SizeD(
            original.width * photo.cropRect.width / photo.transform.zoom,
            original.height * photo.cropRect.height / photo.transform.zoom
        )
        val dpi = PhysicalMeasurementService.effectiveDPI(usedPixelSize, project.photoSize.physicalSize)
        if (dpi <= 0 || dpi >= PrintlyConstants.RECOMMENDED_DPI) return null
        return "Low resolution (${dpi.toInt()} DPI) — may look blurry when printed at this size."
    }

    val hasAnyLowResolutionPhoto: Boolean get() = project.photos.any { qualityWarning(it) != null }

    // MARK: - Export

    fun exportPDF(cacheDir: File) {
        if (project.photos.isEmpty()) {
            errorMessage = "Add at least one photo before exporting."
            return
        }
        isExporting = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val data = PDFExportService.exportPDF(pages.map { it.toLayoutResult() }, project.backgroundColor, project.showCutGuides) { photoID ->
                        project.photos.firstOrNull { it.id == photoID }?.let { resolvedImage(it) }
                    }
                    val file = File(cacheDir, "${project.name}-${UUID.randomUUID()}.pdf")
                    file.writeBytes(data)
                    file
                }
            }
            isExporting = false
            result.onSuccess { file ->
                exportedPDFFile = file
                ExportHistoryStore.shared.record(listOf(file), project.name, ExportedFileFormat.PDF)
            }.onFailure { errorMessage = "Couldn't export PDF: ${it.message}" }
        }
    }

    fun exportImages(cacheDir: File, format: ImageExportService.Format, dpi: Double = PrintlyConstants.RECOMMENDED_DPI) {
        if (project.photos.isEmpty()) {
            errorMessage = "Add at least one photo before exporting."
            return
        }
        isExporting = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val datas = ImageExportService.exportImageData(pages.map { it.toLayoutResult() }, project.backgroundColor, format, dpi, project.showCutGuides) { photoID ->
                        project.photos.firstOrNull { it.id == photoID }?.let { resolvedImage(it) }
                    }
                    val baseName = project.name.trim().ifEmpty { "Printly" }
                    datas.mapIndexed { index, data ->
                        val suffix = if (datas.size > 1) "-page${index + 1}" else ""
                        val file = File(cacheDir, "$baseName$suffix-${UUID.randomUUID()}.${format.extension}")
                        file.writeBytes(data)
                        file
                    }
                }
            }
            isExporting = false
            result.onSuccess { files ->
                exportedImageFiles = files
                val historyFormat = if (format == ImageExportService.Format.JPEG) ExportedFileFormat.JPG else ExportedFileFormat.PNG
                ExportHistoryStore.shared.record(files, project.name, historyFormat)
            }.onFailure { errorMessage = "Couldn't export images: ${it.message}" }
        }
    }

    fun clearExportedFiles() {
        exportedPDFFile = null
        exportedImageFiles = emptyList()
    }
}

/** UI-facing mirror of `LayoutResult` using Compose-observable `data class`
 * copies of placements (Compose can't efficiently observe a plain Kotlin
 * `List<PlacedPhoto>` mutation the way `@Published` arrays work in Swift, so
 * this wraps each page's placement list as its own stable value). */
data class LayoutResultUi(
    val pageSize: SizeD,
    val printableRect: RectD,
    val photoSize: SizeD,
    val margins: PageMargins,
    val spacing: PageSpacing,
    val columns: Int,
    val rows: Int,
    val maxCapacity: Int,
    val requestedCount: Int,
    val placements: List<PlacedPhoto>
) {
    fun toLayoutResult() = com.a8000053398.printly.model.LayoutResult(pageSize, printableRect, photoSize, margins, spacing, columns, rows, maxCapacity, requestedCount, placements)

    companion object {
        fun from(result: com.a8000053398.printly.model.LayoutResult) = LayoutResultUi(
            result.pageSize, result.printableRect, result.photoSize, result.margins, result.spacing,
            result.columns, result.rows, result.maxCapacity, result.requestedCount, result.placements
        )

        fun empty(project: PrintProject): LayoutResultUi {
            val layout = PageLayoutEngine.layout(project.pageSize.pointSize, project.margins, project.photoSize.pointSize, project.spacing, emptyList())
            return from(layout)
        }
    }
}
