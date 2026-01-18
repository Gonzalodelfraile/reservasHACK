package edu.ucam.reservashack.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.ucam.reservashack.domain.model.DaySlots
import edu.ucam.reservashack.domain.model.LibraryService
import edu.ucam.reservashack.domain.model.TableParser
import edu.ucam.reservashack.domain.model.TableSlot
import edu.ucam.reservashack.domain.repository.LibraryRepository
import edu.ucam.reservashack.domain.usecase.RequireActiveAccountUseCase
import edu.ucam.reservashack.ui.shared.SharedEventViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val sharedEventViewModel: SharedEventViewModel,
    private val requireActiveAccount: RequireActiveAccountUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state = _state.asStateFlow()

    // Estado de la selección del usuario
    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate = _selectedDate.asStateFlow()

    private val _selectedTimeId = MutableStateFlow<String?>(null)
    val selectedTimeId = _selectedTimeId.asStateFlow()

    // Filtros de mesas
    private val _showOnlyAvailable = MutableStateFlow(true)
    val showOnlyAvailable = _showOnlyAvailable.asStateFlow()

    private val _minRowNumber = MutableStateFlow<Int?>(null)
    val minRowNumber = _minRowNumber.asStateFlow()

    private val _maxRowNumber = MutableStateFlow<Int?>(null)
    val maxRowNumber = _maxRowNumber.asStateFlow()

    // Estado de UI (mensajes, eventos one-time)
    private val _uiEvent = MutableStateFlow<ReservationUiEvent?>(null)
    val uiEvent = _uiEvent.asStateFlow()

    fun setSelectedDate(date: String?) {
        _selectedDate.value = date
    }

    fun setSelectedTimeId(timeId: String?) {
        _selectedTimeId.value = timeId
    }

    fun setShowOnlyAvailable(show: Boolean) {
        _showOnlyAvailable.value = show
    }

    fun setMinRowNumber(minRow: Int?) {
        _minRowNumber.value = minRow
    }

    fun setMaxRowNumber(maxRow: Int?) {
        _maxRowNumber.value = maxRow
    }

    fun clearUiEvent() {
        _uiEvent.value = null
    }

    init {
        loadData()
        // Escuchar eventos de cambio de cuenta para recargar los datos
        viewModelScope.launch {
            sharedEventViewModel.reloadDataEvent.collect {
                loadData()
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = HomeState.Loading

            try {
                // Validar que hay una cuenta activa
                requireActiveAccount()

                // 1. Pedimos la info estática (Nombres de mesas, horarios generales)
                val libraryResult = repository.getLibraryInfo()

                if (libraryResult.isSuccess) {
                    val library = libraryResult.getOrThrow()

                    // 2. Pedimos la disponibilidad dinámica (Huecos libres/ocupados)
                    val availabilityResult = repository.getAvailability(library.id)

                    if (availabilityResult.isSuccess) {
                        val availability = availabilityResult.getOrThrow()

                        // Auto-seleccionar el primer día y la primera hora disponible para que no salga vacío
                        val firstDate = availability.keys.sorted().firstOrNull()
                        _selectedDate.value = firstDate
                        _selectedTimeId.value = availability[firstDate]?.timeSlots?.keys?.sorted()?.firstOrNull()

                        _state.value = HomeState.Success(library, availability)
                    } else {
                        _state.value = HomeState.Error("Error cargando disponibilidad: ${availabilityResult.exceptionOrNull()?.message}")
                    }
                } else {
                    _state.value = HomeState.Error("Error cargando biblioteca: ${libraryResult.exceptionOrNull()?.message}")
                }
            } catch (e: IllegalStateException) {
                // Error de validación de cuenta activa
                _state.value = HomeState.Error(e.message ?: "Error de validación")
            }
        }
    }

    // Función auxiliar para obtener el estado combinado de una mesa específica
    fun getTableStatus(tableName: String): TableStatusUI {
        val currentState = _state.value
        if (currentState !is HomeState.Success) return TableStatusUI.UNKNOWN

        // Buscamos en el mapa de disponibilidad usando las claves seleccionadas
        val dayData = currentState.availability[_selectedDate.value] ?: return TableStatusUI.UNKNOWN
        val slotList = dayData.timeSlots[_selectedTimeId.value] ?: return TableStatusUI.UNKNOWN

        // Buscamos la mesa específica en esa lista
        val tableStatus = slotList.find { it.name == tableName } ?: return TableStatusUI.UNKNOWN

        return if (tableStatus.isFree) TableStatusUI.FREE else TableStatusUI.OCCUPIED
    }

    /**
     * Genera un mapa de estado de todas las mesas para la fecha y hora seleccionadas.
     * Esta lógica de negocio debe estar en el ViewModel, no en la UI.
     */
    fun getStatusMap(): Map<String, TableStatusUI> {
        val currentState = _state.value
        if (currentState !is HomeState.Success) return emptyMap()

        val currentDate = _selectedDate.value
        val currentTimeId = _selectedTimeId.value
        val slots = currentState.availability[currentDate]?.timeSlots?.get(currentTimeId) ?: emptyList()

        return currentState.library.tables.associate { table ->
            val slot = slots.find { it.name == table.name }
            table.name to if (slot?.isFree == true) TableStatusUI.FREE else TableStatusUI.OCCUPIED
        }
    }

    /**
     * Filtra y ordena las mesas según los criterios seleccionados.
     * La lógica de filtrado pertenece a la capa de presentación (ViewModel), no a la View.
     */
    fun getFilteredTables(): List<TableSlot> {
        val currentState = _state.value
        if (currentState !is HomeState.Success) return emptyList()

        val statusMap = getStatusMap()
        val showAvailable = _showOnlyAvailable.value
        val minRow = _minRowNumber.value
        val maxRow = _maxRowNumber.value

        return currentState.library.tables.filter { table ->
            // Filtro 1: Si solo disponibles está activado, solo mesas libres
            if (showAvailable) {
                val status = statusMap[table.name] ?: TableStatusUI.UNKNOWN
                if (status != TableStatusUI.FREE) return@filter false
            }

            // Filtro 2: Si hay rango de filas, aplicarlo
            val tableRow = TableParser.extractRowNumber(table.name) ?: return@filter false

            if (minRow != null && tableRow < minRow) return@filter false
            if (maxRow != null && tableRow > maxRow) return@filter false

            true
        }.sortedWith { a, b ->
            // Ordenar por número de fila primero, luego por número de mesa
            val parsedA = TableParser.parse(a.name)
            val parsedB = TableParser.parse(b.name)

            if (parsedA == null || parsedB == null) {
                return@sortedWith 0
            }

            val rowComparison = parsedA.rowNumber.compareTo(parsedB.rowNumber)
            if (rowComparison != 0) return@sortedWith rowComparison

            parsedA.tableNumber.compareTo(parsedB.tableNumber)
        }
    }

    /**
     * Obtiene las filas disponibles en la biblioteca para el filtro.
     * Lógica de dominio que debe estar en el ViewModel.
     */
    fun getAvailableRows(): List<Int> {
        val currentState = _state.value
        if (currentState !is HomeState.Success) return emptyList()

        return currentState.library.tables
            .mapNotNull { table -> TableParser.extractRowNumber(table.name) }
            .distinct()
            .sorted()
    }

    fun reserveTable(tableId: Int, tableName: String) {
        viewModelScope.launch {
            val currentState = _state.value
            val currentDate = _selectedDate.value
            val currentTimeId = _selectedTimeId.value
            
            if (currentState is HomeState.Success && currentDate != null && currentTimeId != null) {

                // Buscamos los horarios reales (start/end) de esa mesa
                val slots = currentState.availability[currentDate]?.timeSlots?.get(currentTimeId)
                val tableInfo = slots?.find { it.id == tableId } ?: return@launch

                // Emitir evento de carga
                _uiEvent.value = ReservationUiEvent.Loading("Reservando $tableName...")

                val result = repository.bookTable(
                    serviceId = currentState.library.id,
                    tableId = tableId,
                    date = currentDate,
                    start = tableInfo.startTime,
                    end = tableInfo.endTime
                )

                if (result.isSuccess) {
                    _uiEvent.value = ReservationUiEvent.Success("¡RESERVA CONFIRMADA!")
                    // Recargamos los datos para que la mesa aparezca roja
                    loadData()
                    // Notificar a otras pantallas que se hizo una nueva reserva
                    sharedEventViewModel.notifyReservationMade()
                } else {
                    _uiEvent.value = ReservationUiEvent.Error("Error: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }
}

// Estados de la UI
sealed class HomeState {
    object Loading : HomeState()
    data class Error(val msg: String) : HomeState()
    data class Success(
        val library: LibraryService,
        val availability: Map<String, DaySlots>
    ) : HomeState()
}

// Eventos one-time para la UI (Toasts, Snackbars, etc.)
sealed class ReservationUiEvent {
    data class Loading(val message: String) : ReservationUiEvent()
    data class Success(val message: String) : ReservationUiEvent()
    data class Error(val message: String) : ReservationUiEvent()
}

enum class TableStatusUI { FREE, OCCUPIED, UNKNOWN }