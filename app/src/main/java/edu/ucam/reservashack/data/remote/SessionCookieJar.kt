package edu.ucam.reservashack.data.remote

import edu.ucam.reservashack.domain.model.Session
import edu.ucam.reservashack.domain.repository.SessionRepository
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SessionCookieJar(
    private val sessionRepository: SessionRepository
) : CookieJar {

    companion object {
        // 4.8 hours in milliseconds
        private const val SESSION_DURATION_MS = 17_400_000L
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        var newSessionCookie: String? = null
        var newXsrfToken: String? = null

        for (cookie in cookies) {
            when (cookie.name) {
                "takeaspot_session" -> newSessionCookie = cookie.value
                "XSRF-TOKEN" -> newXsrfToken = cookie.value
            }
        }

        if (newSessionCookie != null || newXsrfToken != null) {
            val currentSession = sessionRepository.getSession()
            
            val finalSessionCookie = newSessionCookie ?: currentSession?.sessionCookie
            val finalXsrfToken = newXsrfToken ?: currentSession?.xsrfToken
            
            if (finalSessionCookie != null) {
                val expiresAt = if (newSessionCookie != null) {
                    System.currentTimeMillis() + SESSION_DURATION_MS
                } else {
                    currentSession?.expiresAt ?: (System.currentTimeMillis() + SESSION_DURATION_MS)
                }

                val session = Session(
                    sessionCookie = finalSessionCookie,
                    xsrfToken = finalXsrfToken,
                    expiresAt = expiresAt
                )
                sessionRepository.saveSession(session)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val session = sessionRepository.getSession() ?: return emptyList()

        // Reconstruimos las cookies para OkHttp
        val cookies = mutableListOf<Cookie>()

        // 1. La cookie de sesión principal
        cookies.add(
            Cookie.Builder()
                .name("takeaspot_session")
                .value(session.sessionCookie)
                .domain(url.host)
                .build()
        )

        // 2. La cookie del token XSRF (necesaria para validación)
        session.xsrfToken?.let { token ->
            cookies.add(
                Cookie.Builder()
                    .name("XSRF-TOKEN")
                    .value(token)
                    .domain(url.host)
                    .build()
            )
        }

        return cookies
    }
}