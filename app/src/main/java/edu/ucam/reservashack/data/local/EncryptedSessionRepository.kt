package edu.ucam.reservashack.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import edu.ucam.reservashack.domain.model.Session
import edu.ucam.reservashack.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class EncryptedSessionRepository @Inject constructor(
    context: Context
) : SessionRepository {

    // 1. Creamos la llave maestra vinculada al hardware del teléfono
    private val masterKey = try {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    } catch (e: Exception) {
        // En caso de error (ej: dispositivo sin soporte), usar clave por defecto
        // Esto es un fallback - en producción considera manejar esto de forma diferente
        throw IllegalStateException("No se pudo inicializar el cifrado seguro", e)
    }

    // 2. Creamos el archivo de preferencias cifrado
    private val sharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "secure_session_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        throw IllegalStateException("No se pudo crear las preferencias cifradas", e)
    }

    // StateFlow para la cuenta activa
    private val _activeAccountId = MutableStateFlow<String?>(
        sharedPreferences.getString("active_account_id", null)
    )

    override fun saveSession(session: Session) {
        sharedPreferences.edit().apply {
            putString("cookie", session.sessionCookie)
            putString("xsrf", session.xsrfToken)
            putLong("expires", session.expiresAt)
            apply()
        }
    }

    override fun getSession(): Session? {
        return try {
            val cookie = sharedPreferences.getString("cookie", null) ?: return null
            val xsrf = sharedPreferences.getString("xsrf", null)
            val expires = sharedPreferences.getLong("expires", 0)

            // Si la cookie existe pero ya expiró, limpiamos y retornamos null
            if (expires > 0 && expires < System.currentTimeMillis()) {
                clearSession()
                return null
            }

            Session(cookie, xsrf, expires)
        } catch (e: Exception) {
            // Si hay error al leer, limpiar datos corruptos
            clearSession()
            null
        }
    }

    override fun clearSession() {
        sharedPreferences.edit().clear().apply()
        _activeAccountId.value = null
    }

    override fun setActiveAccountId(accountId: String) {
        sharedPreferences.edit().putString("active_account_id", accountId).apply()
        _activeAccountId.value = accountId
    }

    override fun getActiveAccountId(): StateFlow<String?> {
        return _activeAccountId
    }

    override fun clearActiveAccountId() {
        sharedPreferences.edit().remove("active_account_id").apply()
        _activeAccountId.value = null
    }
}