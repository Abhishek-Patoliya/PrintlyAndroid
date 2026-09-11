package com.a8000053398.printly.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Ballot
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Textsms
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps this app's icon identifiers (kept identical to the iOS SF Symbol
 * names throughout the data/model layer, e.g. `PrintTemplate.iconName`) to a
 * Material icon — the platform-appropriate visual equivalent, since SF
 * Symbols aren't available on Android.
 */
object IconMapping {
    fun icon(name: String): ImageVector = when (name) {
        "house.fill" -> Icons.Filled.Home
        "square.grid.2x2.fill", "square.grid.2x2" -> Icons.Filled.GridView
        "gearshape.fill" -> Icons.Filled.Settings

        "person.crop.rectangle" -> Icons.Filled.Person
        "photo.fill", "photo" -> Icons.Filled.Photo
        "square.grid.3x3.fill", "square.grid.3x3" -> Icons.Filled.GridOn
        "ruler.fill", "ruler" -> Icons.Filled.SquareFoot
        "tag.fill", "tag" -> Icons.Filled.LocalOffer
        "rectangle.split.3x3.fill" -> Icons.Filled.ViewColumn

        "person.crop.square" -> Icons.Filled.Person
        "airplane.departure" -> Icons.Filled.Flight
        "person.text.rectangle" -> Icons.Filled.Ballot
        "doc.text.image" -> Icons.Filled.Description
        "car.fill" -> Icons.Filled.DirectionsCar
        "square" -> Icons.Outlined.Circle
        "rectangle.portrait" -> Icons.Filled.Photo
        "doc.richtext", "doc.richtext.fill" -> Icons.Filled.Description
        "shippingbox" -> Icons.Filled.LocalShipping
        "envelope" -> Icons.Filled.Mail
        "seal" -> Icons.Filled.CheckCircle
        "doc" -> Icons.Filled.Description
        "doc.badge.clock" -> Icons.Filled.History

        "photo.on.rectangle.angled", "photo.on.rectangle.fill" -> Icons.Filled.PhotoLibrary
        "aspectratio" -> Icons.Filled.AspectRatio
        "square.on.square", "plus.square.on.square" -> Icons.Filled.ContentCopy
        "arrow.left.and.right" -> Icons.Filled.SwapHoriz
        "square.dashed" -> Icons.Filled.Crop
        "ellipsis.circle" -> Icons.Filled.MoreHoriz

        "paintpalette" -> Icons.Filled.Palette
        "square.and.arrow.down" -> Icons.Filled.Save
        "checkmark.circle", "checkmark.circle.fill" -> Icons.Filled.CheckCircle

        "pencil" -> Icons.Filled.Edit
        "rotate.right" -> Icons.AutoMirrored.Filled.RotateRight
        "trash" -> Icons.Filled.Delete
        "doc.on.doc" -> Icons.Filled.ContentCopy

        "qrcode" -> Icons.Filled.QrCode
        "barcode" -> Icons.Filled.HorizontalRule
        "globe" -> Icons.Filled.Language

        "chevron.right" -> Icons.Filled.ChevronRight
        "chevron.left" -> Icons.Filled.ChevronLeft
        "xmark" -> Icons.Filled.Close
        "xmark.circle" -> Icons.Filled.Close
        "photo.badge.plus" -> Icons.Filled.AddAPhoto
        "hand.draw" -> Icons.Filled.Gesture
        "exclamationmark.triangle.fill", "exclamationmark.triangle" -> Icons.Filled.Warning
        "printer.fill" -> Icons.Filled.Print
        "folder" -> Icons.Filled.Folder
        "square.and.arrow.up" -> Icons.Filled.Share
        "doc.badge.plus" -> Icons.Filled.FileDownload
        "arrow.uturn.backward.circle" -> Icons.AutoMirrored.Filled.Undo
        "arrow.uturn.forward.circle" -> Icons.AutoMirrored.Filled.Redo

        "camera.fill" -> Icons.Filled.CameraAlt
        "textformat" -> Icons.Filled.Textsms
        "photo.stack" -> Icons.Filled.Layers
        "arrow.counterclockwise", "arrow.counterclockwise.circle" -> Icons.Filled.Restore
        "list.number" -> Icons.AutoMirrored.Filled.List
        "circle.grid.cross" -> Icons.Filled.Add
        "flip.horizontal", "flip.vertical" -> Icons.Filled.Cameraswitch
        "arrow.triangle.2.circlepath" -> Icons.Filled.FlipCameraAndroid
        "faceid" -> Icons.Filled.Face
        "slash.circle" -> Icons.Filled.Close
        "info.circle" -> Icons.Filled.Info
        "checkmark" -> Icons.Filled.Check
        "circle" -> Icons.Outlined.Circle
        "sparkles" -> Icons.Filled.AutoAwesome
        "timer" -> Icons.Filled.Timer
        "sell" -> Icons.Filled.Sell
        "slider.horizontal.3" -> Icons.Filled.Tune

        else -> Icons.Filled.Photo
    }
}
