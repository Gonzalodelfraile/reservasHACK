package edu.ucam.reservashack.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import edu.ucam.reservashack.domain.model.BookingStatus

/**
 * Clase de datos para los colores de un estado de reserva.
 * @param backgroundColor Color de fondo del badge
 * @param textColor Color del texto del badge
 */
data class BookingStatusColors(
    val backgroundColor: Color,
    val textColor: Color
)

/**
 * Extensión para obtener los colores apropiados según el estado de la reserva.
 * Centraliza la lógica de mapeo de colores y soporte para tema oscuro.
 */
@Composable
fun BookingStatus.getColors(): BookingStatusColors {
    val isDarkTheme = isSystemInDarkTheme()
    return when (this) {
        BookingStatus.ACEPTADO -> BookingStatusColors(
            backgroundColor = if (isDarkTheme) BookingAcceptedDarkBg else BookingAcceptedLightBg,
            textColor = if (isDarkTheme) BookingAcceptedDarkText else BookingAcceptedLightText
        )
        BookingStatus.DENTRO -> BookingStatusColors(
            backgroundColor = if (isDarkTheme) BookingCheckedInDarkBg else BookingCheckedInLightBg,
            textColor = if (isDarkTheme) BookingCheckedInDarkText else BookingCheckedInLightText
        )
        BookingStatus.AUSENTE -> BookingStatusColors(
            backgroundColor = if (isDarkTheme) BookingAbsentDarkBg else BookingAbsentLightBg,
            textColor = if (isDarkTheme) BookingAbsentDarkText else BookingAbsentLightText
        )
        BookingStatus.TERMINADO -> BookingStatusColors(
            backgroundColor = if (isDarkTheme) BookingCompletedDarkBg else BookingCompletedLightBg,
            textColor = if (isDarkTheme) BookingCompletedDarkText else BookingCompletedLightText
        )
        BookingStatus.CANCELADO -> BookingStatusColors(
            backgroundColor = if (isDarkTheme) BookingCancelledDarkBg else BookingCancelledLightBg,
            textColor = if (isDarkTheme) BookingCancelledDarkText else BookingCancelledLightText
        )
        BookingStatus.UNKNOWN -> BookingStatusColors(
            backgroundColor = if (isDarkTheme) BookingCompletedDarkBg else BookingCompletedLightBg,
            textColor = if (isDarkTheme) BookingCompletedDarkText else BookingCompletedLightText
        )
    }
}
