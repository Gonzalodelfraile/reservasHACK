package edu.ucam.reservashack.ui.shared

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ViewModel compartido para comunicar eventos entre pantallas.
 * Se usa para notificar cuando se hace una nueva reserva,
 * de modo que la pantalla de "Mis Reservas" pueda actualizarse.
 * Y para notificar cuando se cambia de cuenta para recargar datos.
 */
@Singleton
class SharedEventViewModel @Inject constructor() : ViewModel() {

    private val _reservationMadeEvent = MutableSharedFlow<Unit>()
    val reservationMadeEvent = _reservationMadeEvent.asSharedFlow()

    // replay=1 para que nuevos colectores reciban el último evento al suscribirse
    private val _reloadDataEvent = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    val reloadDataEvent = _reloadDataEvent.asSharedFlow()

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent = _logoutEvent.asSharedFlow()

    // Evento cuando la sesión expira y requiere re-login (con accountId)
    private val _sessionExpiredEvent = MutableSharedFlow<String>()
    val sessionExpiredEvent = _sessionExpiredEvent.asSharedFlow()

    suspend fun notifyReservationMade() {
        _reservationMadeEvent.emit(Unit)
    }

    suspend fun emitReloadData() {
        _reloadDataEvent.emit(Unit)
    }

    suspend fun emitLogout() {
        _logoutEvent.emit(Unit)
    }

    suspend fun emitSessionExpired(accountId: String) {
        _sessionExpiredEvent.emit(accountId)
    }
}