package edu.ucam.reservashack.domain.repository

import edu.ucam.reservashack.domain.model.UserAccount
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    // Gestión de cuentas en Firestore
    fun getUserAccounts(): Flow<List<UserAccount>>
    suspend fun addAccount(account: UserAccount)
    suspend fun removeAccount(accountId: String)
    suspend fun updateAccount(account: UserAccount)

    // Gestión de la cuenta activa localmente
    fun getActiveAccountId(): Flow<String?>
    suspend fun setActiveAccount(accountId: String)
    suspend fun loadAccountSession(accountId: String)
}