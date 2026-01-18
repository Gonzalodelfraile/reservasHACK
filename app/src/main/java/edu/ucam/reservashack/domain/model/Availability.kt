package edu.ucam.reservashack.domain.model

data class DaySlots(
    val date: String,
    val timeSlots: Map<String, List<TableStatus>> // "1" -> [Mesa1(Libre), Mesa2(Ocupada)]
)

data class TableStatus(
    val id: Int,
    val name: String,
    val isFree: Boolean,
    val startTime: String,
    val endTime: String
)