package edu.ucam.reservashack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.ucam.reservashack.domain.repository.AccountRepository
import edu.ucam.reservashack.domain.repository.SessionRepository
import edu.ucam.reservashack.domain.usecase.CheckSessionUseCase
import edu.ucam.reservashack.ui.shared.SharedEventViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val checkSessionUseCase: CheckSessionUseCase,
    private val auth: FirebaseAuth,
    private val sharedEventViewModel: SharedEventViewModel,
    private val sessionRepository: SessionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()
    
    // Flag para deshabilitar el authStateListener durante logout
    private var isProcessingLogout = false

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        // Si estamos procesando un logout, ignoramos el listener
        if (isProcessingLogout) {
            return@AuthStateListener
        }
        
        viewModelScope.launch {
            // Pequeño delay para asegurar que clearSession() se complete
            delay(100)
            val isFirebaseLoggedIn = firebaseAuth.currentUser != null
            
            if (!isFirebaseLoggedIn) {
                // Si Firebase dice que no estamos loggeados, entonces NO estamos loggeados
                // No importa si hay sesión interna
                sessionRepository.clearSession()
                _isLoggedIn.value = false
            } else {
                // Si Firebase dice que estamos loggeados, cargar sesión interna
                loadActiveAccountSession()
                val isInternalSessionValid = checkSessionUseCase()
                _isLoggedIn.value = isFirebaseLoggedIn || isInternalSessionValid
            }
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        checkSession()
        observeLogoutEvents()
    }

    private fun observeLogoutEvents() {
        viewModelScope.launch {
            sharedEventViewModel.logoutEvent.collect {
                // Se recibió un evento de logout
                isProcessingLogout = true
                
                // Limpiamos la sesión
                sessionRepository.clearSession()
                
                // Establecemos logout inmediatamente
                _isLoggedIn.value = false
                
                // Esperamos un poco antes de re-habilitar el listener
                delay(1000)
                isProcessingLogout = false
            }
        }
    }

    private fun checkSession() {
        viewModelScope.launch {
            // Verificamos tanto la sesión de Firebase como la sesión interna (cookies)
            val isFirebaseLoggedIn = auth.currentUser != null
            
            if (isFirebaseLoggedIn) {
                // Cargar cuenta activa y sus cookies
                loadActiveAccountSession()
            }
            
            val isInternalSessionValid = checkSessionUseCase()
            _isLoggedIn.value = isFirebaseLoggedIn || isInternalSessionValid
        }
    }

    private suspend fun loadActiveAccountSession() {
        try {
            // Obtener el ID de cuenta activa
            val activeAccountId = accountRepository.getActiveAccountId().first()
            
            if (activeAccountId != null) {
                // Cargar la sesión de esa cuenta
                accountRepository.loadAccountSession(activeAccountId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onLoginSuccess() {
        viewModelScope.launch {
            // Cargar cuenta activa después del login
            loadActiveAccountSession()
            _isLoggedIn.value = true
        }
    }

    fun forceLogout() {
        _isLoggedIn.value = false
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }
}