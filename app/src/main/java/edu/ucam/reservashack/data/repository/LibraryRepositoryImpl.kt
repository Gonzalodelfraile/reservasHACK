package edu.ucam.reservashack.data.repository

import android.util.Log
import edu.ucam.reservashack.data.remote.TakeASpotApi
import edu.ucam.reservashack.data.remote.ApiErrorHandler
import edu.ucam.reservashack.data.remote.dto.toDomain
import edu.ucam.reservashack.domain.model.DaySlots
import edu.ucam.reservashack.domain.model.LibraryService
import edu.ucam.reservashack.domain.model.TableStatus
import edu.ucam.reservashack.domain.repository.AccountRepository
import edu.ucam.reservashack.domain.repository.LibraryRepository
import edu.ucam.reservashack.ui.shared.SharedEventViewModel
import javax.inject.Inject
import com.google.gson.Gson
import edu.ucam.reservashack.data.remote.dto.MultiBookingItem
import edu.ucam.reservashack.domain.model.MyBooking
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import java.time.LocalDateTime

class LibraryRepositoryImpl @Inject constructor(
    private val api: TakeASpotApi,
    private val sharedEventViewModel: SharedEventViewModel,
    private val accountRepository: AccountRepository,
    private val errorHandler: ApiErrorHandler
) : LibraryRepository {

    override suspend fun getLibraryInfo(): Result<LibraryService> {
        return try {
            val response = api.getServices()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                
                // Validar que tengamos datos
                if (body.data.isEmpty()) {
                    return Result.failure(Exception("¿No hay servicios disponibles"))
                }
                
                // Buscamos la biblioteca de Murcia (ID 845) o cogemos la primera
                val libraryDto = body.data.find { it.id == 845 } ?: body.data.first()
                Result.success(libraryDto.toDomain())
            } else {
                Result.failure(Exception("Error API: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("LibraryRepo", "Error obteniendo info de biblioteca", e)
            Result.failure(e)
        }
    }

    override suspend fun getAvailability(serviceId: Int): Result<Map<String, DaySlots>> {
        return try {
            val response = api.getServiceSlots(serviceId)

            if (response.isSuccessful && response.body() != null) {
                val rootObject = response.body()!!
                val domainMap = mutableMapOf<String, DaySlots>()

                // 1. Entramos en 'data'
                if (rootObject.has("data") && rootObject.get("data").isJsonObject) {
                    val dataObject = rootObject.getAsJsonObject("data")

                    // 2. Entramos en 'freeslots' (Aquí es donde fallaba antes)
                    if (dataObject.has("freeslots") && dataObject.get("freeslots").isJsonObject) {
                        val freeslotsObject = dataObject.getAsJsonObject("freeslots")

                        Log.d("REPO_DEBUG", "Fechas encontradas: ${freeslotsObject.keySet()}")

                        // 3. Recorremos cada Fecha (ej: "2026-01-07")
                        freeslotsObject.entrySet().forEach { dateEntry ->
                            val dateKey = dateEntry.key // "2026-01-07"
                            val timeSlotsJson = dateEntry.value

                            if (timeSlotsJson.isJsonArray) {
                                val timeSlotsArray = timeSlotsJson.asJsonArray
                                val slotsForThisDay = mutableMapOf<String, List<TableStatus>>()

                                // 4. Recorremos los horarios de ese día (Array)
                                timeSlotsArray.forEach { slotElement ->
                                    val slotObj = slotElement.asJsonObject

                                    // Datos del horario
                                    val start = if (slotObj.has("start")) slotObj.get("start").asString else "00:00"
                                    val end = if (slotObj.has("end")) slotObj.get("end").asString else "00:00"

                                    // Usamos el horario real como ID para consistencia entre fechas
                                    val timeId = "$start-$end"

                                    // 5. Entramos en 'free' (Donde están las mesas)
                                    val tablesList = mutableListOf<TableStatus>()
                                    if (slotObj.has("free") && slotObj.get("free").isJsonObject) {
                                        val freeMap = slotObj.getAsJsonObject("free")

                                        freeMap.entrySet().forEach { tableEntry ->
                                            val tableObj = tableEntry.value.asJsonObject

                                            // Extraemos datos de la mesa
                                            val tableName = if (tableObj.has("name")) tableObj.get("name").asString else "Sin nombre"
                                            val status = if (tableObj.has("status")) tableObj.get("status").asInt else 1
                                            val tableId = if (tableObj.has("id")) tableObj.get("id").asInt else -1

                                            // Asumimos que status 0 es LIBRE (basado en tu json)
                                            val isFree = (status == 0)

                                            tablesList.add(TableStatus(
                                                id = tableId,
                                                name = tableName,
                                                isFree = isFree,
                                                startTime = start,
                                                endTime = end
                                            ))
                                        }
                                    }
                                    slotsForThisDay[timeId] = tablesList
                                }

                                // Guardamos el día procesado
                                domainMap[dateKey] = DaySlots(dateKey, slotsForThisDay)
                            }
                        }
                    } else {
                        Log.e("REPO_DEBUG", "No se encontró el campo 'freeslots' dentro de data")
                    }
                }

                Result.success(domainMap)
            } else {
                Result.failure(Exception("Error API: ${response.code()}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun bookTable(
        serviceId: Int,
        tableId: Int,
        date: String,
        start: String,
        end: String
    ): Result<Int> {
        return try {
            // Validaciones previas
            if (date.isBlank() || start.isBlank() || end.isBlank()) {
                return Result.failure(Exception("Datos de reserva inválidos"))
            }
            
            // 1. Preparar los datos en formato Multipart (texto plano)
            val textType = "text/plain".toMediaTypeOrNull()

            val peoplePart = "1".toRequestBody(textType)
            val datePart = date.toRequestBody(textType) // "2026-01-07"
            val hourPart = "$start-$end".toRequestBody(textType) // "08:30-10:30"
            val servicePart = serviceId.toString().toRequestBody(textType)
            val pitchPart = tableId.toString().toRequestBody(textType)

            Log.d("LibraryRepo", "Reservando: service=$serviceId, table=$tableId, date=$date, time=$start-$end")
            
            // 2. Disparar
            val response = api.makeBooking(peoplePart, datePart, hourPart, servicePart, pitchPart)

            if (response.isSuccessful && response.body() != null) {
                val json = response.body()!!
                // Si el JSON tiene "status": "ok",  ha ido bien
                if (validateJsonOkStatus(json)) {
                    if(json.has("data") && json.get("data").isJsonObject){
                        val dataObj = json.getAsJsonObject("data")
                        val bookingId = if(dataObj.has("booking_id")) dataObj.get("booking_id").asInt else -1
                        Result.success(bookingId)
                    } else {
                        Result.success(-1)
                    }
                } else {
                    Result.failure(Exception("El servidor respondió pero no confirmó la reserva"))
                }
            } else {
                Result.failure(errorHandler.handleHttpError(response.code()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun extendBooking(originalBookingId: Int, items: List<MultiBookingItem>): Result<Boolean> {
        return try {
            val textType = "text/plain".toMediaTypeOrNull()

            // 1. Convertimos la lista de objetos a JSON String
            val gson = Gson()
            val jsonString = gson.toJson(items)
            // jsonString será algo como: [{"date":"...","pitch":"215"}]

            // 2. Creamos los Parts
            val idPart = originalBookingId.toString().toRequestBody(textType)
            val mbDataPart = jsonString.toRequestBody(textType)

            // 3. Llamamos a la API
            val response = api.makeMultiBooking(idPart, mbDataPart)

            if (response.isSuccessful && response.body() != null) {
                val json = response.body()!!
                if (validateJsonOkStatus(json)) {
                    // El servidor devuelve un array con los nuevos IDs en 'data', pero nos basta con saber que es OK
                    Result.success(true)
                } else {
                    Result.failure(Exception("Error en multireserva"))
                }
            } else {
                Result.failure(errorHandler.handleHttpError(response.code(), "Error HTTP Multireserva: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyBookings(): Result<List<MyBooking>> {
        return try {
            val response = api.getBookingsHtml()

            if (response.isSuccessful && response.body() != null) {
                val html = response.body()!!.string()
                val doc = Jsoup.parse(html)

                val bookingList = mutableListOf<MyBooking>()
                var currentDate = "Fecha desconocida"

                // Buscamos todos los elementos de la lista (asumimos que están en <li>)
                val listItems = doc.select("li")

                for (item in listItems) {
                    // CASO 1: Es una cabecera de fecha (tiene clase "col-full")
                    // <div class="col col-full">Miércoles, 07/01/2026</div>
                    val dateHeader = item.select(".col-full").text()
                    if (dateHeader.isNotEmpty()) {
                        currentDate = dateHeader
                        continue
                    }

                    // CASO 2: Es una fila de reserva (tiene clase "table-row")
                    if (item.hasClass("table-row")) {
                        try {
                            // Validar que el data-id no sea una plantilla sin renderizar
                            val dataId = item.attr("data-id")
                            if (dataId.isEmpty() || dataId.contains("{{") || dataId.contains("}}")) {
                                continue // Saltear plantillas no renderizadas
                            }
                            
                            val id = dataId.toInt()
                            val startTime = item.select(".fromtime").text()
                            val endTime = item.select(".totime").text()

                            // La ubicación y mesa están juntas, hay que limpiar
                            // <div class="booking-l"> BIBLIOTECA... <p>Fila...</p></div>
                            val locationRaw = item.select(".booking-l").text() // "BIBLIOTECA MURCIA Fila 11 - Mesa 11"
                            val tableName = item.select(".booking-l p"
                            ).text() // "Fila 11 - Mesa 11"
                            val location = locationRaw.replace(tableName, "").trim()

                            val statusText = item.select(".col-6").text().trim() // "Aceptado", "Dentro", etc.

                            bookingList.add(MyBooking(
                                id = id,
                                date = currentDate,
                                startTime = startTime,
                                endTime = endTime,
                                location = location,
                                tableName = tableName,
                                statusText = statusText,
                                reservationTime = LocalDateTime.now()  // Capturar timestamp actual al obtener reservas
                            ))
                        } catch (e: Exception) {
                            Log.e("HTML_PARSER", "Error parseando una fila: ${e.message}")
                        }
                    }
                }

                Result.success(bookingList)
            } else {
                Result.failure(errorHandler.handleHttpError(response.code()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelBooking(bookingId: Int): Result<Boolean> {
        return try {
            val textType = "text/plain".toMediaTypeOrNull()
            val idPart = bookingId.toString().toRequestBody(textType)

            val response = api.cancelBooking(idPart)

            if (response.isSuccessful && response.body() != null) {
                val json = response.body()!!
                // Nos fiamos del status "ok", ignoramos "data.result"
                if (validateJsonOkStatus(json)) {
                    Result.success(true)
                } else {
                    val msg = if (json.has("message")) json.get("message").asString else "Error desconocido"
                    Result.failure(Exception(msg))
                }
            } else {
                Result.failure(errorHandler.handleHttpError(response.code()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkinBooking(bookingId: Int, people: Int, freeCapacity: Boolean): Result<Boolean> {
        return try {
            val textType = "text/plain".toMediaTypeOrNull()
            val peoplePart = people.toString().toRequestBody(textType)
            val bookingIdPart = bookingId.toString().toRequestBody(textType)
            val freeCapacityPart = freeCapacity.toString().toRequestBody(textType)

            val response = api.makeCheckin(peoplePart, bookingIdPart, freeCapacityPart)

            if (response.isSuccessful && response.body() != null) {
                val json = response.body()!!
                // Validamos status "ok" y data.result == true
                if (validateJsonOkStatus(json)) {
                    val dataObject = json.getAsJsonObject("data")
                    val result = if (dataObject?.has("result") == true) {
                        dataObject.get("result").asBoolean
                    } else {
                        false
                    }
                    
                    if (result) {
                        Result.success(true)
                    } else {
                        Result.failure(Exception("El check-in no se completó correctamente"))
                    }
                } else {
                    val msg = if (json.has("message")) json.get("message").asString else "Error desconocido"
                    Result.failure(Exception(msg))
                }
            } else {
                Result.failure(errorHandler.handleHttpError(response.code()))
            }
        } catch (e: Exception) {
            Log.e("LibraryRepo", "Error en check-in", e)
            Result.failure(e)
        }
    }

    /**
     * Helper para validar respuestas JSON con status "ok"
     * Reduce duplicación en bookTable, extendBooking, cancelBooking, checkinBooking
     */
    private fun validateJsonOkStatus(json: com.google.gson.JsonObject): Boolean {
        return json.has("status") && json.get("status").asString == "ok"
    }
}




