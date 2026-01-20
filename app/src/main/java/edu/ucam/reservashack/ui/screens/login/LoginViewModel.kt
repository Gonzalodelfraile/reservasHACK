package edu.ucam.reservashack.ui.screens.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.ucam.reservashack.domain.model.UserAccount
import edu.ucam.reservashack.domain.repository.AccountRepository
import edu.ucam.reservashack.domain.usecase.ProcessLoginUseCase
import edu.ucam.reservashack.ui.shared.SharedEventViewModel
import edu.ucam.reservashack.ui.shared.LoginState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val processLoginUseCase: ProcessLoginUseCase,
    private val accountRepository: AccountRepository,
    private val sharedEventViewModel: SharedEventViewModel,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Modo: "first_time" = primer login, "add_account" = añadir cuenta adicional, "relogin_account" = re-login de cuenta expirada
    private val loginMode: String = savedStateHandle.get<String>("mode") ?: "first_time"
    private val reloginAccountId: String? = savedStateHandle.get<String>("accountId")

    private val _loginEvent = MutableSharedFlow<Boolean>()
    val loginEvent = _loginEvent.asSharedFlow()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun onCookiesCaptured(cookieString: String, htmlContent: String?) {
        viewModelScope.launch {
            _loginState.value = LoginState.Processing
            
            try {
                when (loginMode) {
                    "add_account" -> {
                        // Modo: Añadir cuenta adicional
                        saveAccountToFirestore(cookieString, htmlContent)
                    }
                    "relogin_account" -> {
                        // Modo: Re-login de cuenta expirada
                        if (reloginAccountId != null) {
                            refreshExpiredAccount(reloginAccountId, cookieString, htmlContent)
                        } else {
                            _loginState.value = LoginState.Error("Error: ID de cuenta no disponible")
                        }
                    }
                    else -> {
                        // Modo: Primer login (comportamiento original)
                        val success = processLoginUseCase(cookieString, htmlContent)
                        if (success) {
                            // Emitir evento de recarga para que las pantallas se actualicen
                            // (la nueva cuenta se activa automáticamente en processLoginUseCase)
                            sharedEventViewModel.emitReloadData()
                            _loginState.value = LoginState.Success
                            _loginEvent.emit(true)
                        } else {
                            _loginState.value = LoginState.Error("Error al procesar el login")
                        }
                    }
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Error desconocido")
            }
        }
    }
    
    private suspend fun refreshExpiredAccount(accountId: String, cookieString: String, @Suppress("UNUSED_PARAMETER") htmlContent: String?) {
        try {
            // Calcular cuándo expira la sesión (4.8 horas)
            val sessionExpiresAt = System.currentTimeMillis() + 17_400_000L

            // Obtener la cuenta existente para preservar datos (usando first())
            val accounts = accountRepository.getUserAccounts().first()
            val existingAccount = accounts.find { it.id == accountId }

            if (existingAccount == null) {
                _loginState.value = LoginState.Error("Cuenta no encontrada")
                return
            }

            // Actualizar la cuenta con las nuevas cookies manteniendo datos existentes
            val updatedAccount = existingAccount.copy(
                cookieString = cookieString,
                sessionExpiresAt = sessionExpiresAt,
                lastUsedAt = System.currentTimeMillis()
            )

            // Actualizar la cuenta en Firestore
            accountRepository.updateAccount(updatedAccount)

            // Cargar la sesión de esta cuenta
            accountRepository.loadAccountSession(accountId)

            // Establecer como activa (esto también recarga la sesión)
            accountRepository.setActiveAccount(accountId)

            _loginState.value = LoginState.Success
            _loginEvent.emit(true)
        } catch (e: Exception) {
            _loginState.value = LoginState.Error("Error al actualizar la sesión: ${e.message}")
        }
    }

    private suspend fun saveAccountToFirestore(cookieString: String, htmlContent: String?) {
        try {
            // Extraer email del HTML si es posible (mejora futura)
            val email = extractEmailFromHtml(htmlContent) ?: "usuario@ucam.edu"
            
            // Calcular cuándo expira la sesión (4.8 horas)
            val sessionExpiresAt = System.currentTimeMillis() + 17_400_000L

            // Crear cuenta con alias temporal
            val newAccount = UserAccount(
                alias = "Cuenta ${System.currentTimeMillis() % 1000}",
                email = email,
                cookieString = cookieString,
                sessionExpiresAt = sessionExpiresAt
            )
            
            accountRepository.addAccount(newAccount)

            // Emitir evento de recarga para que otras pantallas se actualicen
            // (la nueva cuenta se activa automáticamente en addAccount)
            sharedEventViewModel.emitReloadData()

            _loginState.value = LoginState.Success
            _loginEvent.emit(true)
        } catch (e: IllegalStateException) {
            // Máximo de cuentas alcanzado
            _loginState.value = LoginState.Error("Máximo de 4 cuentas alcanzado")
        } catch (e: Exception) {
            _loginState.value = LoginState.Error("Error al guardar la cuenta: ${e.message}")
        }
    }
    
    private fun extractEmailFromHtml(@Suppress("UNUSED_PARAMETER") html: String?): String? {
        // TODO: Parsear el HTML para extraer el email del usuario
        // Por ahora retornamos null
        return null
    }
}

