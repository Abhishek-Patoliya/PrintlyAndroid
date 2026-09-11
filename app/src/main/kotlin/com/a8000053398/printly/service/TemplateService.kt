package com.a8000053398.printly.service

import com.a8000053398.printly.model.MeasurementUnit
import com.a8000053398.printly.model.PageMargins
import com.a8000053398.printly.model.PageOrientation
import com.a8000053398.printly.model.PageSize
import com.a8000053398.printly.model.PageSizePreset
import com.a8000053398.printly.model.PageSpacing
import com.a8000053398.printly.model.PhotoSize
import com.a8000053398.printly.model.PhotoSizePreset
import com.a8000053398.printly.model.PhysicalSize
import com.a8000053398.printly.model.PrintProject
import com.a8000053398.printly.model.PrintTemplate
import com.a8000053398.printly.model.TemplateCategory
import com.a8000053398.printly.service.layout.CollageLayoutService

/** Provides the built-in library of print templates (page + photo size + spacing presets).
 * `iconName` values are kept identical to the iOS app's SF Symbol names; see
 * `ui.theme.IconMapping` for how each one resolves to a Material icon on Android. */
object TemplateService {

    private fun mm(value: Double) = PhysicalMeasurementService.points(value, MeasurementUnit.MILLIMETER)
    private fun inch(value: Double) = PhysicalMeasurementService.points(value, MeasurementUnit.INCH)

    private val passportPhotos = PrintTemplate(
        name = "Passport Photos (A4)", iconName = "person.crop.square", category = TemplateCategory.ID_AND_DOCUMENTS,
        pageSize = PageSize.standard(PageSizePreset.A4), photoSize = PhotoSize.standard(PhotoSizePreset.PASSPORT),
        margins = PageMargins.uniform(mm(10.0)), spacing = PageSpacing.uniform(mm(3.0)), suggestedCopies = 8
    )
    private val visaPhotos = PrintTemplate(
        name = "Visa Photos (A4)", iconName = "airplane.departure", category = TemplateCategory.ID_AND_DOCUMENTS,
        pageSize = PageSize.standard(PageSizePreset.A4), photoSize = PhotoSize.standard(PhotoSizePreset.PASSPORT),
        margins = PageMargins.uniform(mm(10.0)), spacing = PageSpacing.uniform(mm(3.0)), suggestedCopies = 8
    )
    private val idPhotos = PrintTemplate(
        name = "ID Photos 35×45mm (A4)", iconName = "person.text.rectangle", category = TemplateCategory.ID_AND_DOCUMENTS,
        pageSize = PageSize.standard(PageSizePreset.A4), photoSize = PhotoSize.standard(PhotoSizePreset.ID_PHOTO),
        margins = PageMargins.uniform(mm(10.0)), spacing = PageSpacing.uniform(mm(3.0)), suggestedCopies = 8
    )
    private val resumePhotos = PrintTemplate(
        name = "Resume Photos 3×4cm (A4)", iconName = "doc.text.image", category = TemplateCategory.ID_AND_DOCUMENTS,
        pageSize = PageSize.standard(PageSizePreset.A4), photoSize = PhotoSize.standard(PhotoSizePreset.CM_3X4),
        margins = PageMargins.uniform(mm(10.0)), spacing = PageSpacing.uniform(mm(3.0)), suggestedCopies = 8
    )
    private val panCardPhotos = PrintTemplate(
        name = "PAN Card Photos (A4)", iconName = "person.text.rectangle", category = TemplateCategory.ID_AND_DOCUMENTS,
        pageSize = PageSize.standard(PageSizePreset.A4), photoSize = PhotoSize.standard(PhotoSizePreset.PAN_CARD),
        margins = PageMargins.uniform(mm(10.0)), spacing = PageSpacing.uniform(mm(3.0)), suggestedCopies = 8
    )
    private val drivingLicencePhotos = PrintTemplate(
        name = "Driving Licence Photos (A4)", iconName = "car.fill", category = TemplateCategory.ID_AND_DOCUMENTS,
        pageSize = PageSize.standard(PageSizePreset.A4), photoSize = PhotoSize.standard(PhotoSizePreset.DRIVING_LICENCE),
        margins = PageMargins.uniform(mm(10.0)), spacing = PageSpacing.uniform(mm(3.0)), suggestedCopies = 8
    )

    private val oneByOnePrint = PrintTemplate(
        name = "1 × 1 Print", iconName = "square", category = TemplateCategory.PHOTO_PRINTS,
        pageSize = PageSize(PageSizePreset.CUSTOM, PhysicalSize(1.0, 1.0, MeasurementUnit.INCH), PageOrientation.PORTRAIT),
        photoSize = PhotoSize.standard(PhotoSizePreset.ONE_BY_ONE), margins = PageMargins.zero, spacing = PageSpacing.zero, suggestedCopies = 1
    )
    private val eightByTenPrint = PrintTemplate(
        name = "8 × 10 Print", iconName = "rectangle.portrait", category = TemplateCategory.PHOTO_PRINTS,
        pageSize = PageSize(PageSizePreset.CUSTOM, PhysicalSize(8.0, 10.0, MeasurementUnit.INCH), PageOrientation.PORTRAIT),
        photoSize = PhotoSize.standard(PhotoSizePreset.EIGHT_BY_TEN), margins = PageMargins.zero, spacing = PageSpacing.zero, suggestedCopies = 1
    )
    private val twoByTwoWalletSheet = PrintTemplate(
        name = "2 × 2 Wallet Sheet (4×6)", iconName = "square", category = TemplateCategory.PHOTO_PRINTS,
        pageSize = PageSize.standard(PageSizePreset.FOUR_BY_SIX), photoSize = PhotoSize.standard(PhotoSizePreset.TWO_BY_TWO),
        margins = PageMargins.uniform(inch(0.2)), spacing = PageSpacing.uniform(inch(0.1)), suggestedCopies = 4
    )
    private val twoByTwoPrint = PrintTemplate(
        name = "2 × 2 Print", iconName = "square", category = TemplateCategory.PHOTO_PRINTS,
        pageSize = PageSize(PageSizePreset.CUSTOM, PhysicalSize(2.0, 2.0, MeasurementUnit.INCH), PageOrientation.PORTRAIT),
        photoSize = PhotoSize.standard(PhotoSizePreset.TWO_BY_TWO), margins = PageMargins.zero, spacing = PageSpacing.zero, suggestedCopies = 1
    )
    private val fourBySixPrint = PrintTemplate(
        name = "4 × 6 Print", iconName = "photo", category = TemplateCategory.PHOTO_PRINTS,
        pageSize = PageSize.standard(PageSizePreset.FOUR_BY_SIX), photoSize = PhotoSize.standard(PhotoSizePreset.FOUR_BY_SIX),
        margins = PageMargins.zero, spacing = PageSpacing.zero, suggestedCopies = 1
    )
    private val fiveBySevenPrint = PrintTemplate(
        name = "5 × 7 Print", iconName = "rectangle.portrait", category = TemplateCategory.PHOTO_PRINTS,
        pageSize = PageSize.standard(PageSizePreset.FIVE_BY_SEVEN), photoSize = PhotoSize.standard(PhotoSizePreset.FIVE_BY_SEVEN),
        margins = PageMargins.zero, spacing = PageSpacing.zero, suggestedCopies = 1
    )
    private val sixByEightPrint = PrintTemplate(
        name = "6 × 8 Print", iconName = "rectangle.portrait", category = TemplateCategory.PHOTO_PRINTS,
        pageSize = PageSize(PageSizePreset.CUSTOM, PhysicalSize(6.0, 8.0, MeasurementUnit.INCH), PageOrientation.PORTRAIT),
        photoSize = PhotoSize.standard(PhotoSizePreset.SIX_BY_EIGHT), margins = PageMargins.zero, spacing = PageSpacing.zero, suggestedCopies = 1
    )
    private val a4Print = PrintTemplate(
        name = "A4 Full-Page Print", iconName = "doc.richtext", category = TemplateCategory.PHOTO_PRINTS,
        pageSize = PageSize.standard(PageSizePreset.A4),
        photoSize = PhotoSize(PhotoSizePreset.CUSTOM, PageSizePreset.A4.basePortraitSize ?: PhysicalSize(210.0, 297.0, MeasurementUnit.MILLIMETER)),
        margins = PageMargins.zero, spacing = PageSpacing.zero, suggestedCopies = 1
    )
    private val a5Print = PrintTemplate(
        name = "A5 Full-Page Print", iconName = "doc.richtext", category = TemplateCategory.PHOTO_PRINTS,
        pageSize = PageSize.standard(PageSizePreset.A5),
        photoSize = PhotoSize(PhotoSizePreset.CUSTOM, PageSizePreset.A5.basePortraitSize ?: PhysicalSize(148.0, 210.0, MeasurementUnit.MILLIMETER)),
        margins = PageMargins.zero, spacing = PageSpacing.zero, suggestedCopies = 1
    )
    private val eightWalletPhotos = PrintTemplate(
        name = "8 Wallet Photos (2×2 in)", iconName = "square.grid.3x3", category = TemplateCategory.PHOTO_PRINTS,
        pageSize = PageSize.standard(PageSizePreset.A4), photoSize = PhotoSize.standard(PhotoSizePreset.TWO_BY_TWO),
        margins = PageMargins.uniform(mm(10.0)), spacing = PageSpacing.uniform(mm(3.0)), suggestedCopies = 8
    )
    private val a4PhotoSheet = PrintTemplate(
        name = "A4 Photo Sheet", iconName = "square.grid.2x2", category = TemplateCategory.PHOTO_PRINTS,
        pageSize = PageSize.standard(PageSizePreset.A4), photoSize = PhotoSize.standard(PhotoSizePreset.FOUR_BY_SIX),
        margins = PageMargins.uniform(mm(10.0)), spacing = PageSpacing.uniform(mm(5.0)), suggestedCopies = 2
    )
    private val letterPhotoSheet = PrintTemplate(
        name = "Letter Photo Sheet", iconName = "square.grid.2x2", category = TemplateCategory.PHOTO_PRINTS,
        pageSize = PageSize.standard(PageSizePreset.LETTER), photoSize = PhotoSize.standard(PhotoSizePreset.FOUR_BY_SIX),
        margins = PageMargins.uniform(inch(0.25)), spacing = PageSpacing.uniform(inch(0.15)), suggestedCopies = 2
    )

    private fun collageTemplate(count: Int): PrintTemplate {
        val pageSize = PageSize.standard(PageSizePreset.A4)
        val margins = PageMargins.uniform(mm(8.0))
        val spacing = PageSpacing.uniform(mm(3.0))
        val (columns, rows) = CollageLayoutService.grid(count, pageIsLandscape = false)
        val cellPoints = CollageLayoutService.cellSize(pageSize.pointSize, margins, spacing, columns, rows)
        val cellPhysical = PhysicalSize(
            width = PhysicalMeasurementService.value(cellPoints.width, MeasurementUnit.MILLIMETER),
            height = PhysicalMeasurementService.value(cellPoints.height, MeasurementUnit.MILLIMETER),
            unit = MeasurementUnit.MILLIMETER
        )
        return PrintTemplate(
            name = "$count-Photo Collage", iconName = "square.grid.3x3", category = TemplateCategory.COLLAGES,
            pageSize = pageSize, photoSize = PhotoSize(PhotoSizePreset.CUSTOM, cellPhysical),
            margins = margins, spacing = spacing, suggestedCopies = 1, fillPageEdgeToEdge = true
        )
    }

    private val addressLabelSheet = PrintTemplate(
        name = "Address Labels — Avery 5160 (30/sheet)", iconName = "tag", category = TemplateCategory.LABELS,
        pageSize = PageSize.standard(PageSizePreset.LETTER), photoSize = PhotoSize(PhotoSizePreset.CUSTOM, PhysicalSize(2.625, 1.0, MeasurementUnit.INCH)),
        margins = PageMargins.uniform(inch(0.19)), spacing = PageSpacing.uniform(inch(0.12)), suggestedCopies = 30
    )
    private val shippingLabelAvery5163 = PrintTemplate(
        name = "Shipping Labels — Avery 5163 (10/sheet)", iconName = "shippingbox", category = TemplateCategory.LABELS,
        pageSize = PageSize.standard(PageSizePreset.LETTER), photoSize = PhotoSize(PhotoSizePreset.CUSTOM, PhysicalSize(4.0, 2.0, MeasurementUnit.INCH)),
        margins = PageMargins.uniform(inch(0.25)), spacing = PageSpacing.uniform(inch(0.05)), suggestedCopies = 10
    )
    private val returnAddressLabelAvery5195 = PrintTemplate(
        name = "Return Address Labels — Avery 5195 (80/sheet)", iconName = "envelope", category = TemplateCategory.LABELS,
        pageSize = PageSize.standard(PageSizePreset.LETTER), photoSize = PhotoSize(PhotoSizePreset.CUSTOM, PhysicalSize(1.75, 0.5, MeasurementUnit.INCH)),
        margins = PageMargins.uniform(inch(0.3)), spacing = PageSpacing.uniform(inch(0.05)), suggestedCopies = 80
    )
    private val shippingLabel = PrintTemplate(
        name = "Shipping Label (4×6)", iconName = "shippingbox", category = TemplateCategory.LABELS,
        pageSize = PageSize.standard(PageSizePreset.FOUR_BY_SIX), photoSize = PhotoSize.standard(PhotoSizePreset.FOUR_BY_SIX),
        margins = PageMargins.zero, spacing = PageSpacing.zero, suggestedCopies = 1
    )
    private val roundStickers = PrintTemplate(
        name = "Round Stickers (2 in)", iconName = "seal", category = TemplateCategory.LABELS,
        pageSize = PageSize.standard(PageSizePreset.A4), photoSize = PhotoSize(PhotoSizePreset.CUSTOM, PhysicalSize(2.0, 2.0, MeasurementUnit.INCH)),
        margins = PageMargins.uniform(mm(8.0)), spacing = PageSpacing.uniform(mm(4.0)), suggestedCopies = 8
    )
    private val roundLabelsAvery22807 = PrintTemplate(
        name = "Round Labels — Avery 22807 (12/sheet)", iconName = "seal", category = TemplateCategory.LABELS,
        pageSize = PageSize.standard(PageSizePreset.LETTER), photoSize = PhotoSize(PhotoSizePreset.CUSTOM, PhysicalSize(2.0, 2.0, MeasurementUnit.INCH)),
        margins = PageMargins.uniform(inch(0.5)), spacing = PageSpacing.uniform(inch(0.7)), suggestedCopies = 12
    )
    private val addressLabelAvery5262 = PrintTemplate(
        name = "Shipping/Address Labels — Avery 5262 (20/sheet)", iconName = "tag", category = TemplateCategory.LABELS,
        pageSize = PageSize.standard(PageSizePreset.LETTER), photoSize = PhotoSize(PhotoSizePreset.CUSTOM, PhysicalSize(4.0, 1.0, MeasurementUnit.INCH)),
        margins = PageMargins.uniform(inch(0.3)), spacing = PageSpacing.uniform(inch(0.1)), suggestedCopies = 20
    )

    val builtInTemplates: List<PrintTemplate> = listOf(
        passportPhotos, visaPhotos, idPhotos, resumePhotos, panCardPhotos, drivingLicencePhotos,
        oneByOnePrint, twoByTwoPrint, twoByTwoWalletSheet, fourBySixPrint, fiveBySevenPrint, sixByEightPrint,
        eightByTenPrint, a4Print, a5Print, eightWalletPhotos, a4PhotoSheet, letterPhotoSheet,
        addressLabelSheet, shippingLabelAvery5163, returnAddressLabelAvery5195, shippingLabel,
        roundStickers, roundLabelsAvery22807, addressLabelAvery5262
    ) + CollageLayoutService.supportedCounts.map(::collageTemplate)

    /** Applies a template's settings onto an existing project, preserving its photos. */
    fun apply(template: PrintTemplate, project: PrintProject): PrintProject {
        var updated = project.copy(
            name = if (project.name == "Untitled Project") template.name else project.name,
            pageSize = template.pageSize,
            photoSize = template.photoSize,
            margins = template.margins,
            spacing = template.spacing,
            fillPageEdgeToEdge = template.fillPageEdgeToEdge,
            templateID = template.id,
            modifiedAt = System.currentTimeMillis()
        )
        updated = when {
            updated.photos.size == 1 -> updated.copy(photos = listOf(updated.photos[0].copy(copies = template.suggestedCopies)))
            updated.photos.isEmpty() -> updated.copy(pendingCopiesForFirstPhoto = template.suggestedCopies)
            else -> updated
        }
        return updated
    }
}
