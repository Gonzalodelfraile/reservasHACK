package edu.ucam.reservashack.data.remote.dto

import com.google.gson.annotations.SerializedName
import edu.ucam.reservashack.domain.model.LibraryService
import edu.ucam.reservashack.domain.model.TableSlot
import edu.ucam.reservashack.domain.model.TimeSlot

data class ServicesResponse(
    val data: List<ServiceDataDto>
)

data class ServiceDataDto(
    val id: Int,
    val name: String?,
    @SerializedName("properties") val properties: ServicePropertiesDto,
    val timetable: Map<String, List<TimetableEntryDto>>?
)

data class ServicePropertiesDto(
    @SerializedName("total_pitches") val capacity: String,
    @SerializedName("pitches_names") val pitches: List<PitchDto>
)

data class PitchDto(
    val name: String,
    val status: String
)

data class TimetableEntryDto(
    val open: String,
    val close: String,
    @SerializedName("_gbid") val gbid: String?
)

// Extension Function para convertir DTO a Domain
fun ServiceDataDto.toDomain(): LibraryService {
    return LibraryService(
        id = this.id,
        name = this.name ?: "Servicio Desconocido",
        capacity = this.properties.capacity.toIntOrNull() ?: 0,
        tables = this.properties.pitches.map {
            TableSlot(it.name, it.status)
        },
        timetable = this.timetable?.mapValues { entry ->
            entry.value.map { TimeSlot(it.open, it.close, it.gbid) }
        } ?: emptyMap()
    )
}