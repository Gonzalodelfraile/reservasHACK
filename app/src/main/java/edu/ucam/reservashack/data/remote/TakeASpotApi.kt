package edu.ucam.reservashack.data.remote

import com.google.gson.JsonObject
import edu.ucam.reservashack.data.remote.dto.ServicesResponse
import edu.ucam.reservashack.data.remote.dto.SlotItemDto
import edu.ucam.reservashack.data.remote.dto.SlotsResponse
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface TakeASpotApi {

    @GET("/myturner/api/get-services")
    suspend fun getServices(): Response<ServicesResponse>

    @GET("/myturner/api/service-slots/{serviceId}")
    suspend fun getServiceSlots(
        @Path("serviceId") serviceId: Int
    ): Response<JsonObject>

    @Multipart
    @POST("/myturner/api/make-booking")
    suspend fun makeBooking(
        @Part("people") people: RequestBody,
        @Part("date") date: RequestBody,
        @Part("hour") hour: RequestBody,       // Ej: "08:30-10:30"
        @Part("service") serviceId: RequestBody,
        @Part("myturn_pitch") pitchId: RequestBody // ID de la mesa
    ): Response<JsonObject>

    @Multipart
    @POST("/myturner/api/make-multi-booking")
    suspend fun makeMultiBooking(
        @Part("bookingId") bookingId: RequestBody,
        @Part("mbdata") mbdata: RequestBody // Esto será el JSON Array convertido a String
    ): Response<JsonObject>

    @GET("/bookings")
    suspend fun getBookingsHtml(): Response<ResponseBody>

    @Multipart
    @POST("/myturner/api/cancel-booking")
    suspend fun cancelBooking(
        @Part("booking") bookingId: RequestBody
    ): Response<JsonObject>

    @Multipart
    @POST("/myturner/api/make-checkin")
    suspend fun makeCheckin(
        @Part("people") people: RequestBody,
        @Part("booking_id") bookingId: RequestBody,
        @Part("freecapacity") freeCapacity: RequestBody
    ): Response<JsonObject>
}