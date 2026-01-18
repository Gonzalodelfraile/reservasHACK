package edu.ucam.reservashack.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import edu.ucam.reservashack.domain.model.UserAccount
import edu.ucam.reservashack.domain.repository.AccountRepository
import edu.ucam.reservashack.domain.repository.SessionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val sessionRepository: SessionRepository // Para gestionar la sesión activa
) : AccountRepository {

    private val userId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")

    private val accountsCollection
        get() = firestore.collection("users").document(userId).collection("accounts")

    override fun getUserAccounts(): Flow<List<UserAccount>> {
        return try {
            accountsCollection.snapshots().map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserAccount::class.java)?.copy(id = doc.id)
                }
                // No reordenar - mantener orden de creación en Firestore
            }
        } catch (e: Exception) {
            flow { emit(emptyList()) }
        }
    }

    override suspend fun addAccount(account: UserAccount) {
        // Verificar límite de 4 cuentas
        val currentAccounts = accountsCollection.get().await()
        if (currentAccounts.size() >= 4) {
            throw IllegalStateException("Máximo de 4 cuentas alcanzado")
        }
        
        // Añadir cuenta a Firestore
        val docRef = accountsCollection.add(account).await()
        
        // Establecerla como cuenta activa automáticamente
        setActiveAccount(docRef.id)
    }

    override suspend fun removeAccount(accountId: String) {
        try {
            // Obtener el ID de la cuenta activa actual (usando first() porque es un Flow)
            val activeAccountId = sessionRepository.getActiveAccountId().first()

            // Eliminar la cuenta de Firestore
            accountsCollection.document(accountId).delete().await()

            // Si era la cuenta activa, limpiar la sesión
            if (activeAccountId == accountId) {
                // Limpiar la sesión actual
                sessionRepository.clearSession()
                // Limpiar el ID de cuenta activa
                sessionRepository.clearActiveAccountId()
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun updateAccount(account: UserAccount) {
        accountsCollection.document(account.id).set(account).await()
    }

    // Gestión local de la cuenta activa usando SharedPreferences o DataStore
    // Por simplicidad, usaremos un Flow simulado o podríamos guardarlo en SessionRepository si lo adaptamos
    // Aquí asumiremos que SessionRepository puede guardar el ID de la cuenta activa también.
    
    // NOTA: Para una implementación real robusta, deberíamos guardar el ID de la cuenta activa en DataStore local.
    // Como SessionRepository ya usa EncryptedSharedPreferences, podemos añadir métodos allí o usarlo aquí.
    
    // Vamos a simularlo usando el SessionRepository existente si tuviera soporte, 
    // pero como no lo tiene, lo implementaremos aquí de forma básica o modificaremos SessionRepository.
    // Para no romper SessionRepository ahora, usaremos un flujo simple basado en memoria o una preferencia simple.
    
    // Mejor enfoque: Modificar SessionRepository para guardar activeAccountId.
    // Asumiremos que se ha modificado o lo haremos en el siguiente paso.
    
    override fun getActiveAccountId(): Flow<String?> {
        return sessionRepository.getActiveAccountId()
    }

    override suspend fun setActiveAccount(accountId: String) {
        // Actualizar timestamp de último uso
        val accountDoc = accountsCollection.document(accountId).get().await()
        accountDoc.toObject(UserAccount::class.java)?.let { account ->
            val updatedAccount = account.copy(
                id = accountId,
                lastUsedAt = System.currentTimeMillis()
            )
            accountsCollection.document(accountId).set(updatedAccount).await()
        }
        
        // Guardar el ID de la cuenta activa
        sessionRepository.setActiveAccountId(accountId)
        
        // Cargar la sesión de esta cuenta
        loadAccountSession(accountId)
    }

    override suspend fun loadAccountSession(accountId: String) {
        try {
            // 1. Obtener la cuenta de Firestore
            val accountDoc = accountsCollection.document(accountId).get().await()
            val account = accountDoc.toObject(UserAccount::class.java)
            
            account?.let {
                // 2. Parsear las cookies guardadas
                val cookieString = it.cookieString
                if (cookieString.isNotEmpty()) {
                    // 3. Extraer session cookie y XSRF token
                    val cookieMap = parseCookieString(cookieString)
                    val sessionCookie = cookieMap["takeaspot_session"]
                    val xsrfToken = cookieMap["XSRF-TOKEN"]
                    
                    if (sessionCookie != null) {
                        // 4. Crear objeto Session y guardarlo
                        val session = edu.ucam.reservashack.domain.model.Session(
                            sessionCookie = sessionCookie,
                            xsrfToken = xsrfToken,
                            expiresAt = System.currentTimeMillis() + 17_400_000L // 4.8 horas
                        )
                        sessionRepository.saveSession(session)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun parseCookieString(cookieString: String): Map<String, String> {
        return cookieString.split(";")
            .map { it.trim().split("=", limit = 2) }
            .filter { it.size == 2 }
            .associate { it[0] to it[1] }
    }
}