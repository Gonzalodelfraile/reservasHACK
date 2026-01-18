package edu.ucam.reservashack.ui.screens.mybookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.ucam.reservashack.domain.model.MyBooking
import edu.ucam.reservashack.domain.repository.LibraryRepository
import edu.ucam.reservashack.domain.usecase.RequireActiveAccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MyBookingsState {
    object Loading : MyBookingsState()
    data class Success(val bookings: List<MyBooking>) : MyBookingsState()
    data class Error(val message: String) : MyBookingsState()
}

@HiltViewModel
class MyBookingsViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val sharedEventViewModel: edu.ucam.reservashack.ui.shared.SharedEventViewModel,
    private val requireActiveAccount: RequireActiveAccountUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<MyBookingsState>(MyBookingsState.Loading)
    val state = _state.asStateFlow()

    init {
        loadBookings()

        // Escuchar eventos de nuevas reservas desde otras pantallas
        viewModelScope.launch {
            sharedEventViewModel.reservationMadeEvent.collect {
                loadBookings()
            }
        }

        // Escuchar cambios de cuenta activa para recargar datos
        viewModelScope.launch {
            sharedEventViewModel.reloadDataEvent.collect {
                loadBookings()
            }
        }
    }

    fun loadBookings() {
        viewModelScope.launch {
            _state.value = MyBookingsState.Loading

            try {
                // Validar que hay una cuenta activa
                requireActiveAccount()

                val result = libraryRepository.getMyBookings()

                result.fold(
                    onSuccess = { bookings ->
                        _state.value = MyBookingsState.Success(bookings)
                    },
                    onFailure = { error ->
                        _state.value = MyBookingsState.Error(error.message ?: "Error al cargar reservas")
                    }
                )
            } catch (e: IllegalStateException) {
                // Error de validación de cuenta activa
                _state.value = MyBookingsState.Error(e.message ?: "Error de validación")
            }
        }
    }

    fun cancelBooking(bookingId: Int) {
        viewModelScope.launch {
            // Opcional: Mostrar estado de carga específico para cancelación
            val result = libraryRepository.cancelBooking(bookingId)
            
            result.fold(
                onSuccess = {
                    // Recargar la lista tras cancelar
                    loadBookings()
                },
                onFailure = {
                    // Manejar error (podría exponerse en otro StateFlow para mostrar un Toast)
                }
            )
        }
    }

    fun checkinBooking(bookingId: Int) {
        viewModelScope.launch {
            val result = libraryRepository.checkinBooking(bookingId, people = 1, freeCapacity = false)
            
            result.fold(
                onSuccess = {
                    // Recargar la lista tras check-in
                    loadBookings()
                },
                onFailure = {
                    // Manejar error (podría exponerse en otro StateFlow para mostrar un Toast)
                }
            )
        }
    }
}