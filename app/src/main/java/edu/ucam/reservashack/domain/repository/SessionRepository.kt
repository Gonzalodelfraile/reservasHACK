package edu.ucam.reservashack.domain.repository

import edu.ucam.reservashack.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun saveSession(session: Session)
    fun getSession(): Session?
    fun clearSession()
    fun setActiveAccountId(accountId: String)
    fun getActiveAccountId(): Flow<String?>
    fun clearActiveAccountId()
}