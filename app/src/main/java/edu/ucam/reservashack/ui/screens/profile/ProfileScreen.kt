package edu.ucam.reservashack.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import edu.ucam.reservashack.R
import edu.ucam.reservashack.domain.model.UserAccount
import edu.ucam.reservashack.ui.navigation.OtherRoutes
import edu.ucam.reservashack.ui.screens.profile.ProfileUiState

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    navController: NavController? = null,
    onAddAccountClick: () -> Unit // Callback para navegación
) {
    val accounts by viewModel.accounts.collectAsState(initial = emptyList())
    val activeAccountId by viewModel.activeAccountId.collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()
    val showReloginDialog by viewModel.showReloginDialog.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    
    // Mostrar mensajes de éxito o error
    LaunchedEffect(uiState) {
        when (val state = uiState) {
        
            is ProfileUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
                viewModel.clearUiState()
            }
            else -> { /* No hacer nada */ }
        }
    }

    // Mostrar diálogo cuando la sesión expire y necesite re-login
    val accountIdToRelogin = showReloginDialog
    if (accountIdToRelogin != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissReloginDialog() },
            title = { Text("Sesión Expirada") },
            text = { Text("Tu sesión ha caducado. Por favor, vuelve a iniciar sesión en esta cuenta.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissReloginDialog()
                        // Navegar a pantalla de re-login con el accountId
                        navController?.navigate(OtherRoutes.getReloginRoute(accountIdToRelogin))
                    }
                ) {
                    Text("Iniciar Sesión")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissReloginDialog() }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                if (accounts.isEmpty()) {
                    // Estado vacío
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_accounts),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val bottomActionsPadding = 128.dp // espacio para botones de agregar y logout
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = bottomActionsPadding)
                    ) {
                        items(accounts, key = { it.id }) { account ->
                            AccountItem(
                                account = account,
                                isActive = account.id == activeAccountId,
                                onActivate = { viewModel.activateAccount(account.id) },
                                onDelete = { viewModel.deleteAccount(account.id) },
                                onEditAlias = { newAlias -> 
                                    viewModel.updateAccountAlias(account.id, newAlias)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (accounts.size < 4) {
                    Button(
                        onClick = onAddAccountClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is ProfileUiState.Loading
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.add_account))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.max_accounts_reached),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    enabled = uiState !is ProfileUiState.Loading
                ) {
                    Text(stringResource(R.string.logout))
                }
            }
            
            // Indicador de carga
            if (uiState is ProfileUiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun AccountItem(
    account: UserAccount,
    isActive: Boolean,
    onActivate: () -> Unit,
    onDelete: () -> Unit,
    onEditAlias: (String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isActive)
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        else
            null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isActive) { onActivate() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .heightIn(min = 72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Avatar con iniciales + Nombre
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar circular con iniciales
                Surface(
                    shape = CircleShape,
                    color = if (isActive) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = getInitials(account.alias.ifEmpty { "U" }),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isActive) Color.White else MaterialTheme.colorScheme.primary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
                
                // Nombre (sin badge "Activa")
                Text(
                    text = account.alias.ifEmpty { stringResource(R.string.account_no_name) },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
            }

            // Iconos de acción
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar nombre",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = stringResource(R.string.delete), 
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
    
    if (showEditDialog) {
        EditAliasDialog(
            currentAlias = account.alias,
            onDismiss = { showEditDialog = false },
            onConfirm = { newAlias ->
                onEditAlias(newAlias)
                showEditDialog = false
            }
        )
    }
}

// Función auxiliar para obtener iniciales
private fun getInitials(name: String): String {
    return name.trim()
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "U" }
}

@Composable
fun EditAliasDialog(
    currentAlias: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var aliasText by remember { mutableStateOf(currentAlias) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar nombre de cuenta", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text(
                    "Ingresa un nuevo nombre para esta cuenta:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = aliasText,
                    onValueChange = { aliasText = it },
                    label = { Text("Nombre", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(aliasText.trim()) },
                enabled = aliasText.trim().isNotEmpty()
            ) {
                Text("Guardar", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}