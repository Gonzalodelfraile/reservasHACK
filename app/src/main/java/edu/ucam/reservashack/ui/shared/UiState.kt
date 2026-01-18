package edu.ucam.reservashack.ui.shared

/**
 * Estado genérico para ViewModels.
 * Centraliza la lógica de estados Loading/Success/Error.
 *
 * @param T Tipo de datos en caso de éxito
 */
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
