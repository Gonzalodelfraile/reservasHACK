package edu.ucam.reservashack.domain.util

/**
 * Clase genérica para manejar estados de operaciones asíncronas de manera consistente.
 * Permite diferenciar entre cargando, éxito y error de forma clara en la UI.
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()

    /**
     * Ejecuta una acción solo si el recurso es exitoso
     */
    inline fun onSuccess(action: (T) -> Unit): Resource<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Ejecuta una acción solo si hay un error
     */
    inline fun onError(action: (String, Throwable?) -> Unit): Resource<T> {
        if (this is Error) action(message, cause)
        return this
    }

    /**
     * Ejecuta una acción solo si está cargando
     */
    inline fun onLoading(action: () -> Unit): Resource<T> {
        if (this is Loading) action()
        return this
    }
}

/**
 * Función auxiliar para convertir Result a Resource de forma segura
 */
fun <T> Result<T>.toResource(): Resource<T> {
    return fold(
        onSuccess = { Resource.Success(it) },
        onFailure = { Resource.Error(it.message ?: "Error desconocido", it) }
    )
}
