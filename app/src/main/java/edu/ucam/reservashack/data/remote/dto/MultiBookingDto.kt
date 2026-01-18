package edu.ucam.reservashack.data.remote.dto

data class MultiBookingItem(
    val date: String,  // "2026-01-07"
    val start: String, // "10:30"
    val end: String,   // "12:30"
    val pitch: String  // "215" (El ID de la mesa como String)
)