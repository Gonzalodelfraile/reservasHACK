package edu.ucam.reservashack.domain.repository

import edu.ucam.reservashack.data.remote.dto.MultiBookingItem
import edu.ucam.reservashack.domain.model.DaySlots
import edu.ucam.reservashack.domain.model.LibraryService
import edu.ucam.reservashack.domain.model.MyBooking

interface LibraryRepository {
    suspend fun getLibraryInfo(): Result<LibraryService>
    suspend fun getAvailability(serviceId: Int): Result<Map<String, DaySlots>>
    suspend fun bookTable(serviceId: Int, tableId: Int, date: String, start: String, end: String): Result<Int>
    suspend fun extendBooking(originalBookingId: Int, items: List<MultiBookingItem>): Result<Boolean>
    suspend fun getMyBookings(): Result<List<MyBooking>>
    suspend fun cancelBooking(bookingId: Int): Result<Boolean>
    suspend fun checkinBooking(bookingId: Int, people: Int = 1, freeCapacity: Boolean = false): Result<Boolean>
}