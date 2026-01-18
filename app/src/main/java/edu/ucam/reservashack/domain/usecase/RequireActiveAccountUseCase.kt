package edu.ucam.reservashack.domain.usecase

import edu.ucam.reservashack.domain.repository.AccountRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case que valida que hay una cuenta activa antes de ejecutar operaciones.
 * Centraliza la lógica repetida en múltiples ViewModels.
 */
class RequireActiveAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Verifica que existe una cuenta activa.
     * @return ID de la cuenta activa
     * @throws IllegalStateException si no hay cuenta activa
     */
    suspend operator fun invoke(): String {
        val activeAccountId = accountRepository.getActiveAccountId().first()
        if (activeAccountId.isNullOrEmpty()) {
            throw IllegalStateException("Por favor, activa una cuenta para continuar")
        }
        return activeAccountId
    }
}
