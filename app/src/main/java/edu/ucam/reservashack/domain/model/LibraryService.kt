package edu.ucam.reservashack.domain.model

data class LibraryService(
    val id: Int,
    val name: String,
    val capacity: Int,
    val tables: List<TableSlot>,
    val timetable: Map<String, List<TimeSlot>> // "monday" -> [8:30-10:30, ...]
)

data class TableSlot(
    val name: String, // "Fila 1;Mesa 01"
    val status: String // "0" = libre (probablemente), "1" = ocupada/bloqueada
) {
    // Función auxiliar para pintar bonito en la UI
    fun getDisplayName(): String = name.replace(";", " - ")
}

data class TimeSlot(
    val open: String,  // "08:30"
    val close: String, // "10:30"
    val id: String? = null // "_gbid", quizás necesario para reservar
)