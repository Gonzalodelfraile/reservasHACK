package edu.ucam.reservashack.data.remote

import edu.ucam.reservashack.domain.repository.SessionRepository
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class XSRFInterceptor(
    private val sessionRepository: SessionRepository
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        // Añadimos headers fijos para parecer un navegador real
        builder.header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
        builder.header("Accept", "application/json, text/plain, */*")
        builder.header("X-Requested-With", "XMLHttpRequest") // Clave para indicar que es una llamada AJAX

        // Magia CSRF: Leemos el token y lo ponemos en la cabecera
        sessionRepository.getSession()?.xsrfToken?.let { encodedToken ->
            // A veces el token viene URL-encoded (%3D), hay que decodificarlo
            try {
                val decodedToken = URLDecoder.decode(encodedToken, StandardCharsets.UTF_8.name())
                builder.header("X-XSRF-TOKEN", decodedToken)
            } catch (e: Exception) {
                // Si falla, enviamos el original
                builder.header("X-XSRF-TOKEN", encodedToken)
            }
        }

        return chain.proceed(builder.build())
    }
}