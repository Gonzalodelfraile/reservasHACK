package edu.ucam.reservashack.ui.screens.mybookings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import edu.ucam.reservashack.R
import edu.ucam.reservashack.domain.model.MyBooking
import edu.ucam.reservashack.domain.model.BookingStatus
import edu.ucam.reservashack.ui.shared.ErrorState
import edu.ucam.reservashack.ui.theme.*

@Composable
fun MyBookingsScreen(
    viewModel: MyBookingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()


    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentState = state) {
            is MyBookingsState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is MyBookingsState.Error -> {
                ErrorState(
                    message = currentState.message,
                    onRetry = { viewModel.loadBookings() },
                    modifier = Modifier.fillMaxSize(),
                    retryLabel = stringResource(R.string.retry)
                )
            }
            is MyBookingsState.Success -> {
                if (currentState.bookings.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.no_active_reservations), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadBookings() }) {
                            Text(stringResource(R.string.refresh), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(currentState.bookings) { booking ->
                            BookingItem(
                                booking = booking,
                                onCancel = { viewModel.cancelBooking(booking.id) },
                                onCheckin = { viewModel.checkinBooking(booking.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingItem(
    booking: MyBooking,
    onCancel: () -> Unit,
    onCheckin: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val accentColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = accentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = booking.date,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Badge de estado
                val colors = booking.status.getColors()
                Surface(
                    color = colors.backgroundColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = booking.statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${booking.startTime} - ${booking.endTime}",
                style = MaterialTheme.typography.headlineSmall,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Texto de ubicación eliminado a petición del usuario
            Text(
                text = booking.tableName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val actionButtonModifier = Modifier.weight(1f).height(48.dp)
                // Botón de check-in solo si puede hacer check-in
                if (booking.canCheckin()) {
                    Button(
                        onClick = onCheckin,
                        modifier = actionButtonModifier,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CheckInButtonColor
                        )
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Check-in",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Botón de cancelar solo si puede cancelar
                if (booking.canCancel) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = actionButtonModifier,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.cancel_reservation),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}