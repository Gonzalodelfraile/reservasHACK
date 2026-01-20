package edu.ucam.reservashack.domain.model

/**
 * Representa una cuenta universitaria guardada en Firebase.
 * La información sensible (cookies) está guardada de forma segura.
 */
data class UserAccount(
    val id: String = "", // ID del documento en Firestore
    val alias: String = "", // Nombre personalizado para mostrar (ej: "Mi cuenta personal")
    val email: String = "", // Email asociado a la cuenta universitaria
    val cookieString: String = "", // String de cookies de sesión (se guarda en Firestore)
    val createdAt: Long = System.currentTimeMillis(), // Timestamp de creación de la cuenta
    val lastUsedAt: Long = System.currentTimeMillis(), // Timestamp del último uso
    val sessionExpiresAt: Long = 0L // Timestamp de expiración de la sesión (0 = expirada)
) {
    // Constructor sin argumentos requerido por Firebase Firestore para deserialización
    // R8 no debe eliminar este constructor en builds release
    constructor() : this(
        id = "",
        alias = "",
        email = "",
        cookieString = "",
        createdAt = System.currentTimeMillis(),
        lastUsedAt = System.currentTimeMillis(),
        sessionExpiresAt = 0L
    )
}
