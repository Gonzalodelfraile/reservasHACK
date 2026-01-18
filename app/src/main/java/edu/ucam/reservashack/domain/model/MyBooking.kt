package edu.ucam.reservashack.domain.model

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class BookingStatus(val displayName: String) {
    ACEPTADO("Aceptado"),
    DENTRO("Dentro"),
    AUSENTE("Ausente"),
    TERMINADO("Terminado"),
    CANCELADO("Cancelado"),
    UNKNOWN("Desconocido");
    
    companion object {
        fun from(text: String): BookingStatus {
            return when (text.trim().lowercase()) {
                "aceptado" -> ACEPTADO
                "dentro" -> DENTRO
                "ausente" -> AUSENTE
                "terminado" -> TERMINADO
                "cancelado" -> CANCELADO
                else -> UNKNOWN
            }
        }
    }
}

data class MyBooking(
    val id: Int,                    // 3284729 (Necesario para cancelar)
    val date: String,               // "Miércoles, 07/01/2026"
    val startTime: String,          // "08:30"
    val endTime: String,            // "10:30"
    val location: String,           // "BIBLIOTECA MURCIA"
    val tableName: String,          // "Fila 11 - Mesa 11"
    val statusText: String,         // "Aceptado", "Dentro", "Ausente", etc.
    val status: BookingStatus = BookingStatus.from(statusText),  // Enum parsed
    val reservationTime: LocalDateTime? = null  // Fecha/hora cuando se hizo la reserva
) {
    // Helper: permite cancelar si es "Aceptado"
    val canCancel: Boolean get() = status == BookingStatus.ACEPTADO
    
    // Helper: permite check-in si es "Aceptado" Y cumple alguna de estas condiciones:
    // 1. Está entre el comienzo de la reserva y media hora después del comienzo
    // 2. Ha reservado durante el periodo de la reserva y no ha pasado media hora después del final
    fun canCheckin(currentTime: LocalDateTime = LocalDateTime.now()): Boolean {
        if (status != BookingStatus.ACEPTADO) return false

        // Parsear startTime y endTime a LocalDateTime
        val today = currentTime.toLocalDate()
        val startDateTime = try {
            val time = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"))
            today.atTime(time)
        } catch (e: Exception) {
            return false
        }

        val endDateTime = try {
            val time = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm"))
            today.atTime(time)
        } catch (e: Exception) {
            return false
        }

        // Opción 1: Está entre startTime y startTime + 30 minutos
        val option1Start = startDateTime
        val option1End = startDateTime.plusMinutes(30)
        if (currentTime >= option1Start && currentTime <= option1End) {
            return true
        }

        // Opción 2: reservationTime está dentro del periodo de reserva
        // y el check-in se hace hasta 30 minutos después del endTime
        if (reservationTime != null &&
            reservationTime >= startDateTime &&
            reservationTime <= endDateTime) {
            val option2End = endDateTime.plusMinutes(30)
            return currentTime <= option2End
        }

        return false
    }
}