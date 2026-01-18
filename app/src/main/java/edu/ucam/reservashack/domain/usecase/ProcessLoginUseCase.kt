package edu.ucam.reservashack.domain.usecase

import edu.ucam.reservashack.domain.model.Session
import edu.ucam.reservashack.domain.model.UserAccount
import edu.ucam.reservashack.domain.repository.AccountRepository
import edu.ucam.reservashack.domain.repository.SessionRepository
import java.util.regex.Pattern
import javax.inject.Inject

class ProcessLoginUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val accountRepository: AccountRepository
) {
    companion object {
        private const val SESSION_COOKIE_NAME = "takeaspot_session"
        private const val XSRF_TOKEN_NAME = "XSRF-TOKEN"
        // 4.8 hours in milliseconds
        private const val SESSION_DURATION_MS = 17_400_000L
        private const val DEFAULT_ACCOUNT_NAME = "Nueva Cuenta UCAM"
    }

    suspend operator fun invoke(cookieString: String, htmlContent: String?): Boolean {
        val cookieMap = parseCookieString(cookieString)

        val sessionCookie = cookieMap[SESSION_COOKIE_NAME]
        val xsrfToken = cookieMap[XSRF_TOKEN_NAME]

        if (sessionCookie != null) {
            val expiresAt = System.currentTimeMillis() + SESSION_DURATION_MS
            val session = Session(
                sessionCookie = sessionCookie,
                xsrfToken = xsrfToken,
                expiresAt = expiresAt
            )
            
            // 1. Guardar sesión localmente (para uso inmediato)
            sessionRepository.saveSession(session)
            
            // 2. Guardar cuenta en Firestore
            val userName = htmlContent?.let { extractUserName(it) } ?: DEFAULT_ACCOUNT_NAME
            
            // TODO: Encriptar sessionCookie antes de guardar
            val newAccount = UserAccount(
                alias = userName,
                email = "", // Pendiente de extraer
                cookieString = cookieString, // Guardamos la cookie completa por ahora
                createdAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis()
            )
            
            try {
                accountRepository.addAccount(newAccount)
                // Obtener el ID de la cuenta recién creada y establecerla como activa
                // Como addAccount no retorna el ID, usaremos el SessionRepository directamente
                // La cuenta se activará cuando el usuario la seleccione manualmente
            } catch (e: Exception) {
                // Si falla guardar en remoto, al menos tenemos la sesión local
                e.printStackTrace()
            }

            return true
        }
        return false
    }

    private fun extractUserName(htmlContent: String): String? {
        val pattern = Pattern.compile("""<div class="dropdown-item info-name">\s*<p>(.*?) -</p>""", Pattern.DOTALL)
        val matcher = pattern.matcher(htmlContent)
        return if (matcher.find()) {
            matcher.group(1)?.trim()
        } else {
            null
        }
    }

    private fun parseCookieString(cookieString: String): Map<String, String> {
        return cookieString.split(";")
            .map { it.trim().split("=", limit = 2) }
            .filter { it.size == 2 }
            .associate { it[0] to it[1] }
    }
}