package edu.ucam.reservashack.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import edu.ucam.reservashack.R

// Fuente Montserrat de Google Fonts (requiere descargar los archivos TTF)
// Ver FONTS_SETUP.md para instrucciones de descarga
val MontserratFontFamily = try {
    FontFamily(
        Font(R.font.montserrat_light, FontWeight.Light),
        Font(R.font.montserrat_regular, FontWeight.Normal),
        Font(R.font.montserrat_medium, FontWeight.Medium),
        Font(R.font.montserrat_semibold, FontWeight.SemiBold),
        Font(R.font.montserrat_bold, FontWeight.Bold)
    )
} catch (e: Exception) {
    // Fallback a fuente del sistema si los archivos no están disponibles
    FontFamily.Default
}

