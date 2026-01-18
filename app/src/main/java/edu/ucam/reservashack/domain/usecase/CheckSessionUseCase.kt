package edu.ucam.reservashack.domain.usecase

import edu.ucam.reservashack.domain.repository.SessionRepository
import javax.inject.Inject

class CheckSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    operator fun invoke(): Boolean {
        val session = sessionRepository.getSession()
        return session?.isValid() == true
    }
}
