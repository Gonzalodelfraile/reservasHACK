package edu.ucam.reservashack.data.remote

import android.util.Log
import edu.ucam.reservashack.domain.repository.AccountRepository
import edu.ucam.reservashack.ui.shared.SharedEventViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manejador centralizado de errores HTTP.
 * Detecta sesiones expiradas (401) y emite eventos para re-autenticación.
 */
@Singleton
class ApiErrorHandler @Inject constructor(
    private val accountRepository: AccountRepository,
    private val sharedEventViewModel: SharedEventViewModel
) {
    /**
     * Maneja errores HTTP y detecta sesiones expiradas (401).
     * @param code Código de estado HTTP
     * @param message Mensaje de error personalizado
     * @return Exception con el mensaje apropiado
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun handleHttpError(code: Int, message: String = "Error HTTP: $code"): Exception {
        if (code == 401) {
            // Sesión expirada - obtener el ID de la cuenta activa y emitir evento
            try {
                // Usar GlobalScope para emitir el evento sin bloquear (es un evento background)
                GlobalScope.launch {
                    try {
                        val activeAccountId = accountRepository.getActiveAccountId().first()
                        if (activeAccountId != null) {
                            sharedEventViewModel.emitSessionExpired(activeAccountId)
                        }
                    } catch (e: Exception) {
                        Log.e("ApiErrorHandler", "Error emitiendo evento de sesión expirada", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("ApiErrorHandler", "Error obteniendo accountId", e)
            }
            return Exception("Sesión expirada. Por favor, vuelve a iniciar sesión.")
        }
        return Exception(message)
    }
}
