package edu.ucam.reservashack.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- COLORES CORPORATIVOS ---
val UcamBlue = Color(0xFF002E6D)
val UcamGold = Color(0xFFE8B00D)
val UcamLightBlue = Color(0xFF00AEEF)

// Variantes generadas para contenedores (hacen que la app no se vea morada)
val UcamBlueContainer = Color(0xFFD4E3FF) // Un azul muy clarito para fondos activos
val OnUcamBlueContainer = Color(0xFF001C48) // Texto oscuro sobre el azul clarito

// Neutros limpios
val BackgroundLight = Color(0xFFF8F9FA) // Gris casi blanco, muy moderno
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE1E2EC) // Gris suave para bordes o tarjetas inactivas
val OnSurfaceVariant = Color(0xFF44474F)    // Gris oscuro para textos secundarios
val OutlineLight = Color(0xFF74777F)

// Constantes para esquema oscuro (evitar hex inline)
val PrimaryContainerDark = Color(0xFF3D5A80) // Azul grisáceo más claro que el fondo
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val OnSurfaceDark = Color(0xFFE1E2E1)
val OnSurfaceVariantDark = Color(0xFF9E9E9E)

// Constantes adicionales esquema claro
val SecondaryContainerLight = Color(0xFFFFEFA8) // Dorado muy pálido opcional
val OnBackgroundLight = Color(0xFF191C20)
val OnSurfaceLight = Color(0xFF191C20)

private val DarkColorScheme = darkColorScheme(
    primary = UcamBlue,
    onPrimary = Color.White,
    // Contenedor más claro para que la opción seleccionada resalte
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = UcamLightBlue, // Texto en azul claro para mejor visibilidad
    secondary = UcamGold,
    background = BackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark // Gris para elementos no seleccionados
)

private val LightColorScheme = lightColorScheme(
    // Colores Principales
    primary = UcamBlue,
    onPrimary = Color.White,
    
    // AQUÍ ESTÁ LA CLAVE: Definir los contenedores para eliminar el morado
    primaryContainer = UcamBlueContainer,
    onPrimaryContainer = OnUcamBlueContainer,

    secondary = UcamGold,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainerLight, // Un dorado muy pálido opcional

    tertiary = UcamLightBlue,

    // Fondos y Superficies
    background = BackgroundLight,
    surface = SurfaceWhite,
    onBackground = OnBackgroundLight,
    onSurface = OnSurfaceLight,

    // Elementos inactivos o variantes (Tarjetas grisaceas)
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariant,
    outline = OutlineLight
)

@Composable
fun ReservasHackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Mantenemos dynamicColor false para forzar la marca UCAM
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            // Iconos blancos en status bar
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false 
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Asegúrate de tener Typography definido en otro lado
        content = content
    )
}