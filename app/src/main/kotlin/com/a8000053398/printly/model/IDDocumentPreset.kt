package com.a8000053398.printly.model

import java.util.UUID

/** A named country/document photo-size entry — a lighter-weight companion to
 * [PhotoSizePreset] for the many national ID/passport/visa formats that don't
 * warrant their own enum case. */
data class IDDocumentPreset(
    val id: UUID = UUID.randomUUID(),
    val country: String,
    val documentName: String,
    val size: PhysicalSize
) {
    val displayName: String get() = "$country — $documentName"
}

object IDDocumentPresetCatalog {
    private val icao35x45mm = PhysicalSize(35.0, 45.0, MeasurementUnit.MILLIMETER)
    private val us2x2in = PhysicalSize(2.0, 2.0, MeasurementUnit.INCH)

    /** Sizes shown here are common defaults sourced from publicly documented national
     * requirements, not guaranteed current or complete — always verify before submitting. */
    val all: List<IDDocumentPreset> = listOf(
        IDDocumentPreset(country = "United States", documentName = "Passport / Visa", size = us2x2in),
        IDDocumentPreset(country = "United States", documentName = "Green Card", size = us2x2in),
        IDDocumentPreset(country = "Canada", documentName = "Passport", size = PhysicalSize(50.0, 70.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "United Kingdom", documentName = "Passport", size = icao35x45mm),
        IDDocumentPreset(country = "United Kingdom", documentName = "Visa", size = icao35x45mm),
        IDDocumentPreset(country = "Ireland", documentName = "Passport", size = icao35x45mm),
        IDDocumentPreset(country = "European Union / Schengen", documentName = "Passport / Visa", size = icao35x45mm),
        IDDocumentPreset(country = "Germany", documentName = "Passport / ID Card (Biometric)", size = icao35x45mm),
        IDDocumentPreset(country = "France", documentName = "Passport / ID Card", size = icao35x45mm),
        IDDocumentPreset(country = "Italy", documentName = "Passport", size = icao35x45mm),
        IDDocumentPreset(country = "Spain", documentName = "Passport / DNI", size = PhysicalSize(30.0, 40.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "Netherlands", documentName = "Passport", size = icao35x45mm),
        IDDocumentPreset(country = "Switzerland", documentName = "Passport", size = icao35x45mm),
        IDDocumentPreset(country = "Sweden", documentName = "Passport", size = icao35x45mm),
        IDDocumentPreset(country = "Poland", documentName = "Passport", size = icao35x45mm),
        IDDocumentPreset(country = "Portugal", documentName = "Passport", size = icao35x45mm),
        IDDocumentPreset(country = "India", documentName = "Passport", size = icao35x45mm),
        IDDocumentPreset(country = "India", documentName = "PAN Card", size = PhysicalSize(25.0, 35.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "India", documentName = "Visa (US)", size = us2x2in),
        IDDocumentPreset(country = "China", documentName = "Passport / Visa", size = PhysicalSize(33.0, 48.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "China", documentName = "ID Card (1-inch)", size = PhysicalSize(25.0, 35.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "China", documentName = "ID Card (2-inch)", size = PhysicalSize(35.0, 49.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "Japan", documentName = "Passport", size = icao35x45mm),
        IDDocumentPreset(country = "Japan", documentName = "Visa", size = PhysicalSize(45.0, 45.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "South Korea", documentName = "Passport", size = PhysicalSize(35.0, 45.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "Vietnam", documentName = "Passport", size = PhysicalSize(40.0, 60.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "Thailand", documentName = "Passport", size = icao35x45mm),
        IDDocumentPreset(country = "Philippines", documentName = "Passport", size = PhysicalSize(35.0, 45.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "Indonesia", documentName = "Passport", size = PhysicalSize(35.0, 45.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "Malaysia", documentName = "Passport", size = icao35x45mm),
        IDDocumentPreset(country = "Singapore", documentName = "Passport", size = PhysicalSize(35.0, 45.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "Australia", documentName = "Passport", size = icao35x45mm),
        IDDocumentPreset(country = "New Zealand", documentName = "Passport", size = icao35x45mm),
        IDDocumentPreset(country = "Brazil", documentName = "Passport", size = PhysicalSize(50.0, 70.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "Mexico", documentName = "Passport", size = PhysicalSize(3.5, 4.5, MeasurementUnit.CENTIMETER)),
        IDDocumentPreset(country = "Argentina", documentName = "Passport", size = PhysicalSize(4.0, 4.0, MeasurementUnit.CENTIMETER)),
        IDDocumentPreset(country = "South Africa", documentName = "Passport", size = icao35x45mm),
        IDDocumentPreset(country = "Nigeria", documentName = "Passport", size = PhysicalSize(35.0, 45.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "Egypt", documentName = "Passport", size = PhysicalSize(40.0, 60.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "Saudi Arabia", documentName = "Passport", size = PhysicalSize(40.0, 60.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "United Arab Emirates", documentName = "Passport / Visa", size = PhysicalSize(43.0, 55.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "Turkey", documentName = "Passport", size = PhysicalSize(50.0, 60.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "Russia", documentName = "Passport", size = PhysicalSize(35.0, 45.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "Pakistan", documentName = "Passport", size = PhysicalSize(35.0, 45.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "Bangladesh", documentName = "Passport", size = PhysicalSize(35.0, 45.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "Sri Lanka", documentName = "Passport", size = PhysicalSize(35.0, 45.0, MeasurementUnit.MILLIMETER)),
        IDDocumentPreset(country = "Nepal", documentName = "Passport", size = PhysicalSize(35.0, 45.0, MeasurementUnit.MILLIMETER))
    )

    fun matching(query: String): List<IDDocumentPreset> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return all
        val lowered = trimmed.lowercase()
        return all.filter { it.country.lowercase().contains(lowered) || it.documentName.lowercase().contains(lowered) }
    }
}
