package edu.ucam.reservashack.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.ucam.reservashack.ui.shared.LoginState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FirebaseLoginViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    fun login(email: String, pass: String) {
        _loginState.value = LoginState.Processing
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                _loginState.value = LoginState.Success
            }
            .addOnFailureListener { e ->
                _loginState.value = LoginState.Error(e.message ?: "Error desconocido")
            }
    }

    fun register(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _loginState.value = LoginState.Error("Email y contraseña son obligatorios")
            return
        }
        
        _loginState.value = LoginState.Processing
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                _loginState.value = LoginState.Success
            }
            .addOnFailureListener { e ->
                _loginState.value = LoginState.Error(e.message ?: "Error al registrarse")
            }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}