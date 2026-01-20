package edu.ucam.reservashack.ui.screens.login

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import edu.ucam.reservashack.ui.shared.LoginState
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    @Suppress("UNUSED_PARAMETER") mode: String = "first_time",
    @Suppress("UNUSED_PARAMETER") accountId: String = "",
    onLoginSuccess: () -> Unit
) {
    var isWebViewVisible by remember { mutableStateOf(true) }
    var cookiesCaptured by remember { mutableStateOf(false) }
    val loginState by viewModel.loginState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loginEvent.collect { success ->
            if (success) {
                // Esperar un poco antes de navegar para que el usuario vea el estado Success
                kotlinx.coroutines.delay(800)
                onLoginSuccess()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            loginState is LoginState.Processing || isWebViewVisible -> {
                // Mostrar WebView durante el procesamiento o captura
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        createLoginWebView(context) { cookies, html ->
                            if (!cookiesCaptured) {
                                cookiesCaptured = true
                                // Ocultamos el WebView inmediatamente al capturar las cookies
                                isWebViewVisible = false
                                viewModel.onCookiesCaptured(cookies, html)
                            }
                        }
                    },
                    update = { webView ->
                        // Controlamos la visibilidad del WebView
                        webView.visibility = if (isWebViewVisible) View.VISIBLE else View.INVISIBLE
                    }
                )
            }
        }
        
        // Mostramos estado de procesamiento o error
        when (val state = loginState) {
            is LoginState.Processing -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Guardando cuenta...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            is LoginState.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Éxito",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "¡Cuenta agregada correctamente!",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }
            is LoginState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            "❌ Error",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                isWebViewVisible = true
                                cookiesCaptured = false
                            }
                        ) {
                            Text("Reintentar", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            else -> { /* Idle */ }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createLoginWebView(context: Context, onLoginComplete: (String, String) -> Unit): WebView {
    val webView = WebView(context)
    
    webView.settings.javaScriptEnabled = true
    webView.settings.domStorageEnabled = true

    webView.addJavascriptInterface(object {
        @android.webkit.JavascriptInterface
        fun processHTML(html: String, cookies: String) {
            // Volvemos al hilo principal para invocar el callback que actualiza estados de Compose
            webView.post {
                onLoginComplete(cookies, html)
            }
        }
    }, "HtmlExtractor")

    clearCookies()

    webView.webViewClient = LoginWebViewClient()
    webView.loadUrl("https://reservas.ucam.edu")
    
    return webView
}

private fun clearCookies() {
    CookieManager.getInstance().removeAllCookies(null)
    CookieManager.getInstance().flush()
}

private class LoginWebViewClient : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (shouldCaptureCookies(url)) {
            val cookies = CookieManager.getInstance().getCookie(url)
            if (cookies != null && view != null) {
                // Inyectamos CSS para ocultar el contenido de la página web original

                // Esto es una capa extra de seguridad visual
                view.evaluateJavascript(
                    "document.body.style.display = 'none';",
                    null
                )
                
                // Pasamos cookies y HTML a la interfaz Java
                // Escapamos comillas simples en las cookies por si acaso
                val safeCookies = cookies.replace("'", "\\'")
                view.loadUrl("javascript:window.HtmlExtractor.processHTML(document.documentElement.outerHTML, '$safeCookies');")
            }
        }
    }

    private fun shouldCaptureCookies(url: String?): Boolean {
        return url != null && url.contains("/myturner")
    }
}