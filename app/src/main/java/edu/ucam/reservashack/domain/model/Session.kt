package edu.ucam.reservashack.domain.model

data class Session(
    val sessionCookie: String,
    val xsrfToken: String?,
    val expiresAt: Long // Timestamp en milisegundos
) {
    // Función auxiliar para saber si ha caducado
    fun isValid(): Boolean {
        return System.currentTimeMillis() < expiresAt
    }
}