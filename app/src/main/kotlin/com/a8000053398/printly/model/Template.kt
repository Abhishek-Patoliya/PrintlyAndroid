package com.a8000053398.printly.model

import com.a8000053398.printly.core.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

/** Groups the built-in template library for the Templates screen. */
@Serializable
enum class TemplateCategory(val displayName: String) {
    ID_AND_DOCUMENTS("ID & Documents"),
    PHOTO_PRINTS("Photo Prints"),
    COLLAGES("Collages"),
    LABELS("Labels & Stickers"),
    MY_TEMPLATES("My Templates");
}

/** A named, reusable combination of page size, photo size, margins, and spacing. */
@Serializable
data class PrintTemplate(
    @Serializable(with = UUIDSerializer::class) val id: UUID = UUID.randomUUID(),
    val name: String,
    /** Material icon name identifier (see `IconNames`), the Android equivalent of the SF Symbol name. */
    val iconName: String,
    val category: TemplateCategory,
    val pageSize: PageSize,
    val photoSize: PhotoSize,
    val margins: PageMargins,
    val spacing: PageSpacing,
    val suggestedCopies: Int,
    /** When true, the layout fills the printable area edge-to-edge instead of centering. */
    val fillPageEdgeToEdge: Boolean = false
)
