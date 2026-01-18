package edu.ucam.reservashack.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.ucam.reservashack.domain.model.UserAccount
import edu.ucam.reservashack.domain.repository.AccountRepository
import edu.ucam.reservashack.domain.repository.SessionRepository
import edu.ucam.reservashack.ui.shared.SharedEventViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val auth: FirebaseAuth,
    private val sessionRepository: SessionRepository,
    private val sharedEventViewModel: SharedEventViewModel
) : ViewModel() {

    val accounts: Flow<List<UserAccount>> = accountRepository.getUserAccounts()
    val activeAccountId: Flow<String?> = accountRepository.getActiveAccountId()
    
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Estado para solicitar re-login cuando la sesión expire
    private val _showReloginDialog = MutableStateFlow<String?>(null)
    val showReloginDialog: StateFlow<String?> = _showReloginDialog.asStateFlow()

    init {
        // Escuchar eventos de sesión expirada
        viewModelScope.launch {
            sharedEventViewModel.sessionExpiredEvent.collect { accountId ->
                _showReloginDialog.value = accountId
            }
        }
    }

    fun dismissReloginDialog() {
        _showReloginDialog.value = null
    }

    fun activateAccount(accountId: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                accountRepository.setActiveAccount(accountId)
                _uiState.value = ProfileUiState.Success("Cuenta activada correctamente")
                // Emitir evento de recarga para que otras pantallas se actualicen
                sharedEventViewModel.emitReloadData()
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Error al activar cuenta")
            }
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                // Obtener el ID de la cuenta activa para verificar si es la que vamos a eliminar
                val activeAccountId = accountRepository.getActiveAccountId().first()

                // Eliminar la cuenta
                accountRepository.removeAccount(accountId)

                // Si era la cuenta activa, emitir evento de recarga para que otras pantallas se actualicen
                if (activeAccountId == accountId) {
                    sharedEventViewModel.emitReloadData()
                }

                _uiState.value = ProfileUiState.Success("Cuenta eliminada")
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Error al eliminar cuenta")
            }
        }
    }

    fun updateAccountAlias(accountId: String, newAlias: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                // Obtener la primera emisión del Flow de cuentas
                val accountsList = accountRepository.getUserAccounts().first()
                val targetAccount = accountsList.find { it.id == accountId }
                
                if (targetAccount != null) {
                    val updatedAccount = targetAccount.copy(alias = newAlias)
                    accountRepository.updateAccount(updatedAccount)
                    _uiState.value = ProfileUiState.Success("Nombre actualizado")
                } else {
                    _uiState.value = ProfileUiState.Error("Cuenta no encontrada")
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Error al actualizar nombre")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                // 1. Limpiamos la sesión interna
                sessionRepository.clearSession()
                
                // 2. Cerramos sesión en Firebase
                // Esto disparará el AuthStateListener en MainActivity que cambiará a la pantalla de login
                auth.signOut()
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error("Error al cerrar sesión: ${e.message}")
            }
        }
    }
    
    fun clearUiState() {
        _uiState.value = ProfileUiState.Idle
    }
}

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(val message: String) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}