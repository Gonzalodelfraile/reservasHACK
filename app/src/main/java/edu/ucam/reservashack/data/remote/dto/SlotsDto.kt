package edu.ucam.reservashack.data.remote.dto

import com.google.gson.annotations.SerializedName

// El contenedor principal
data class SlotsResponse(
    // Clave: Fecha (ej: "2025-01-08")
    // Valor: Mapa de Horas
    val slots: Map<String, Map<String, List<SlotItemDto>>>
)

// El objeto que representa una mesa en un horario concreto
data class SlotItemDto(
    val name: String?,
    val start: String?,
    val end: String?,
    val available: Int? = 0,
    val booked: Int? = 0,
    val blocked: Boolean? = false,
    val capacity: Int? = 1
) {
    fun isBookable(): Boolean {
        // Lógica defensiva: si available es 1 y booked es 0, está libre
        return (available == 1) && (blocked == false) && ((booked ?: 0) < (capacity ?: 1))
    }
}