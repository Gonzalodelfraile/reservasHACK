package edu.ucam.reservashack.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import edu.ucam.reservashack.ui.theme.UcamBlue
import edu.ucam.reservashack.ui.theme.UcamGold

/**
 * Colores estándar para FilterChips de la aplicación.
 * Usa los colores UCAM con soporte para tema oscuro.
 */
@Composable
fun ucamFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surface,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.surface,
    selectedLabelColor = if (isSystemInDarkTheme()) UcamGold else UcamBlue
)

/**
 * Borde animado para FilterChips de la aplicación.
 * Cambia de color según si está seleccionado o no.
 *
 * @param isSelected Si el chip está seleccionado
 */
@Composable
fun ucamFilterChipBorder(isSelected: Boolean): BorderStroke {
    val isDark = isSystemInDarkTheme()
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isDark) UcamGold else UcamBlue
        } else {
            MaterialTheme.colorScheme.outline
        },
        label = "chipBorderColor"
    )
    return BorderStroke(
        width = if (isSelected) 2.dp else 1.dp,
        color = animatedColor
    )
}
