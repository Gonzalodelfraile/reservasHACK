package edu.ucam.reservashack.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.hilt.navigation.compose.hiltViewModel
import edu.ucam.reservashack.domain.model.TableStatus
import edu.ucam.reservashack.ui.shared.ErrorState
import edu.ucam.reservashack.ui.theme.UcamBlue
import edu.ucam.reservashack.ui.theme.UcamGold
import edu.ucam.reservashack.ui.components.ucamFilterChipColors
import edu.ucam.reservashack.ui.components.ucamFilterChipBorder

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedTimeId by viewModel.selectedTimeId.collectAsState()
    val uiEvent by viewModel.uiEvent.collectAsState()

    // Manejar eventos de UI (Toasts)
    val context = LocalContext.current
    LaunchedEffect(uiEvent) {
        uiEvent?.let { event ->
            when (event) {
                is ReservationUiEvent.Loading -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                is ReservationUiEvent.Success -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_LONG).show()
                }
                is ReservationUiEvent.Error -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_LONG).show()
                }
            }
            viewModel.clearUiEvent()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        when (state) {
            is HomeState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is HomeState.Error -> {
                ErrorState(
                    message = (state as HomeState.Error).msg,
                    onRetry = { viewModel.loadData() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            is HomeState.Success -> {
                SuccessContent(
                    state = state as HomeState.Success,
                    selectedDate = selectedDate,
                    selectedTimeId = selectedTimeId,
                    onDateSelected = { viewModel.setSelectedDate(it) },
                    onTimeSelected = { viewModel.setSelectedTimeId(it) },
                    viewModel = viewModel
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SuccessContent(
    state: HomeState.Success,
    selectedDate: String?,
    selectedTimeId: String?,
    onDateSelected: (String) -> Unit,
    onTimeSelected: (String) -> Unit,
    viewModel: HomeViewModel
) {
    // Recopilar estado de filtros del ViewModel
    val showOnlyAvailable by viewModel.showOnlyAvailable.collectAsState()
    val minRowNumber by viewModel.minRowNumber.collectAsState()
    val maxRowNumber by viewModel.maxRowNumber.collectAsState()

    // --- ESTADO PARA EL DIÁLOGO DE CONFIRMACIÓN ---
    var showConfirmDialog by remember { mutableStateOf(false) }
    // Guardamos temporalmente el ID y el Nombre de la mesa que se quiere reservar
    var tablePendingConfirmation by remember { mutableStateOf<Pair<Int, String>?>(null) }

    // --- ESTADO PARA EL MENÚ DE FILTROS ---
    var showFilterMenu by remember { mutableStateOf(false) }

    // DELEGAR LÓGICA AL VIEWMODEL (cumple MVVM)
    // El ViewModel es responsable de la lógica de negocio y filtrado
    val statusMap = remember(selectedDate, selectedTimeId, state) {
        viewModel.getStatusMap()
    }

    val filteredTables = remember(state, showOnlyAvailable, minRowNumber, maxRowNumber, statusMap) {
        viewModel.getFilteredTables()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp)
    ) {
        // 1. Selector de FECHAS
        item {
            Text("Selecciona un día:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                val availableDates = state.availability.keys.sorted()
                items(availableDates) { date ->
                    FilterChip(
                        selected = date == selectedDate,
                        onClick = { onDateSelected(date) },
                        label = { Text(date) },
                        colors = ucamFilterChipColors(),
                        border = ucamFilterChipBorder(date == selectedDate)
                    )
                }
            }
        }

        // 2. Selector de HORAS
        if (selectedDate != null) {
            item {
                Text("Selecciona una hora:", style = MaterialTheme.typography.titleMedium)
            }

            item {
                val dayData = state.availability[selectedDate]
                val timeIds = dayData?.timeSlots?.keys?.sorted() ?: emptyList()

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(timeIds) { timeId ->
                        val timeSlotsForThisHour = dayData?.timeSlots?.get(timeId) ?: emptyList()
                        val realTime = getDisplayTime(timeSlotsForThisHour, timeId)
                        FilterChip(
                            selected = timeId == selectedTimeId,
                            onClick = { onTimeSelected(timeId) },
                            label = { Text(realTime) },
                            colors = ucamFilterChipColors(),
                            border = ucamFilterChipBorder(timeId == selectedTimeId)
                        )
                    }
                }
            }


            // 3. Encabezado y botón de filtros - STICKY HEADER
            stickyHeader {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.background),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Texto dinámico según filtros activos
                    val headerText = remember(showOnlyAvailable, minRowNumber, maxRowNumber) {
                        buildString {
                            append("Mesas")

                            // Filtro de disponibilidad
                            if (showOnlyAvailable) {
                                append(" Disponibles")
                            }

                            // Filtro de rango de filas
                            when {
                                minRowNumber != null && maxRowNumber != null -> {
                                    if (minRowNumber == maxRowNumber) {
                                        append(" (Fila $minRowNumber)")
                                    } else {
                                        append(" (Filas $minRowNumber-$maxRowNumber)")
                                    }
                                }
                                minRowNumber != null -> append(" (Desde Fila $minRowNumber)")
                                maxRowNumber != null -> append(" (Hasta Fila $maxRowNumber)")
                                else -> if (!showOnlyAvailable) append(":")
                            }

                            if (showOnlyAvailable || minRowNumber != null || maxRowNumber != null) {
                                append(":")
                            }
                        }
                    }

                    Text(headerText, style = MaterialTheme.typography.titleMedium)

                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Filtrar mesas")
                    }
                }
            }
        }

        // 4. Mensaje si no hay hora seleccionada
        if (selectedTimeId == null) {
            item {
                Text(
                    "Selecciona una hora para ver disponibilidad",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            // 5. Grid de MESAS - Usar la variable filteredTables calculada arriba

            // Usar un grid dentro del LazyColumn usando items
            items(
                count = (filteredTables.size + 3) / 4, // Calcular número de filas para 4 columnas
                key = { it }
            ) { rowIndex ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(4) { colIndex ->
                        val tableIndex = rowIndex * 4 + colIndex
                        if (tableIndex < filteredTables.size) {
                            val table = filteredTables[tableIndex]
                            val status = statusMap[table.name] ?: TableStatusUI.UNKNOWN
                            val slots = state.availability[selectedDate]?.timeSlots?.get(selectedTimeId)
                            val realTableId = slots?.find { it.name == table.name }?.id ?: -1

                            Box(modifier = Modifier.weight(1f)) {
                                TableCard(
                                    name = table.getDisplayName(),
                                    status = status,
                                    onClick = {
                                        if (status == TableStatusUI.FREE && realTableId != -1) {
                                            tablePendingConfirmation = Pair(realTableId, table.name)
                                            showConfirmDialog = true
                                        }
                                    }
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheet para filtros (FUERA del LazyColumn)
    if (showFilterMenu) {
        ModalBottomSheet(
            onDismissRequest = { showFilterMenu = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    "Filtrar Mesas",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Sección: Disponibilidad
                Text(
                    "Disponibilidad:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable {
                            viewModel.setShowOnlyAvailable(true)
                            showFilterMenu = false
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = showOnlyAvailable,
                        onClick = null
                    )
                    Text("Solo disponibles", style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable {
                            viewModel.setShowOnlyAvailable(false)
                            showFilterMenu = false
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = !showOnlyAvailable,
                        onClick = null
                    )
                    Text("Todas las mesas", style = MaterialTheme.typography.bodyMedium)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Sección: Filtro por Fila
                Text(
                    "Filtrar por Fila:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Extraer números de fila disponibles desde el ViewModel
                val availableRows = remember(state) {
                    viewModel.getAvailableRows()
                }

                // Scroll horizontal de números para fila mínima
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("Desde Fila:", style = MaterialTheme.typography.bodySmall)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        item {
                            FilterNumberButton(
                                number = "Todas",
                                isSelected = minRowNumber == null,
                                onClick = { viewModel.setMinRowNumber(null) }
                            )
                        }
                        items(availableRows) { row ->
                            FilterNumberButton(
                                number = row.toString(),
                                isSelected = minRowNumber == row,
                                onClick = { viewModel.setMinRowNumber(row) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scroll horizontal de números para fila máxima
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("Hasta Fila:", style = MaterialTheme.typography.bodySmall)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        item {
                            FilterNumberButton(
                                number = "Todas",
                                isSelected = maxRowNumber == null,
                                onClick = { viewModel.setMaxRowNumber(null) }
                            )
                        }
                        items(availableRows) { row ->
                            FilterNumberButton(
                                number = row.toString(),
                                isSelected = maxRowNumber == row,
                                onClick = { viewModel.setMaxRowNumber(row) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botones de acción
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.setMinRowNumber(null)
                            viewModel.setMaxRowNumber(null)
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Limpiar")
                    }
                    Button(
                        onClick = { showFilterMenu = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Aplicar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- COMPONENTE ALERT DIALOG (FUERA del LazyColumn) ---
    if (showConfirmDialog && tablePendingConfirmation != null) {
        val (tableId, tableName) = tablePendingConfirmation!!

        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
            title = { Text(text = "Confirmar Reserva") },
            text = {
                Text("¿Estás seguro de que quieres reservar la ${tableName}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reserveTable(tableId, tableName)
                        showConfirmDialog = false
                        tablePendingConfirmation = null
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        tablePendingConfirmation = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun TableCard(
    name: String,
    status: TableStatusUI,
    onClick: () -> Unit
) {
    val isFree = status == TableStatusUI.FREE

    // Tonalidades de grises sin sombra
    val backgroundColor = if (isFree) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }

    val textColor = if (isFree) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    }

    Card(
        modifier = Modifier
            .height(80.dp)
            .clickable(enabled = status == TableStatusUI.FREE, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name
                    .replace("Fila ", "")
                    .replace("Mesa ", "")
                    .replace(";", "\n"),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.3f),
                maxLines = 2
            )
        }
    }
}

// Función para obtener la hora a mostrar
fun getDisplayTime(timeSlots: List<TableStatus>, timeId: String): String {
    // Si timeId tiene el formato "08:30-10:30", extraemos la primera parte
    if (timeId.contains("-")) {
        val parts = timeId.split("-")
        return parts.firstOrNull() ?: timeId
    }
    
    // Si no hay slots, intentamos obtener del primer slot disponible
    if (timeSlots.isEmpty()) return timeId
    
    // Obtener el primer slot
    val firstSlot = timeSlots.firstOrNull() ?: return timeId
    val rawTime = firstSlot.startTime

    // Si el string es largo ("2026-01-07 08:30:00"), lo cortamos.
    // Si es corto ("08:30"), lo devolvemos tal cual.
    return if (rawTime.length >= 16) {
        rawTime.substring(11, 16)
    } else {
        rawTime
    }
}

@Composable
fun FilterNumberButton(
    number: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(number) },
        colors = ucamFilterChipColors(),
        border = ucamFilterChipBorder(isSelected),
        modifier = Modifier.height(40.dp)
    )
}
