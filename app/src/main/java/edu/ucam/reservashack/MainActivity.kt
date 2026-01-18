package edu.ucam.reservashack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import edu.ucam.reservashack.ui.navigation.MainAppScaffold
import edu.ucam.reservashack.ui.screens.login.FirebaseLoginScreen
import edu.ucam.reservashack.ui.theme.ReservasHackTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReservasHackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Observamos directamente el estado de Firebase Auth
                    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

                    // Listener que actualiza el estado cuando Firebase Auth cambia
                    DisposableEffect(Unit) {
                        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                            isLoggedIn = firebaseAuth.currentUser != null
                        }
                        auth.addAuthStateListener(authStateListener)
                        
                        onDispose {
                            auth.removeAuthStateListener(authStateListener)
                        }
                    }

                    if (isLoggedIn) {
                        MainAppScaffold()
                    } else {
                        FirebaseLoginScreen(
                            onLoginSuccess = {
                                // El authStateListener actualizará isLoggedIn automáticamente
                            }
                        )
                    }
                }
            }
        }
    }
}