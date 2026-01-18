package edu.ucam.reservashack.ui.shared

/**
 * Estado compartido para flujos de autenticación.
 * Usado por LoginViewModel y FirebaseLoginViewModel.
 */
sealed class LoginState {
    object Idle : LoginState()
    object Processing : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
