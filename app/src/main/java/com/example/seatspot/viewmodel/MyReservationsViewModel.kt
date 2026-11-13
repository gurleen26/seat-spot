package com.example.seatspot.viewmodel

import androidx.lifecycle.ViewModel
import com.example.seatspot.data.model.MyReservation
import com.example.seatspot.data.model.ReservationParser
import com.example.seatspot.network.SeatSpotApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MyReservationsUiState(
    val isLoading: Boolean = false,
    val reservations: List<MyReservation> = emptyList(),
    val error: String? = null,
    val cancellingReservationId: String? = null,
    val cancellationError: String? = null,
    val cancellationSuccess: String? = null
)

class MyReservationsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MyReservationsUiState())
    val uiState: StateFlow<MyReservationsUiState> = _uiState.asStateFlow()

    fun loadReservations(idToken: String) {
        if (idToken.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Authentication token is missing."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            cancellationError = null
        )

        SeatSpotApi.getMyReservations(idToken) { result ->
            result.onSuccess { response ->
                try {
                    val reservations = ReservationParser.parse(response)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        reservations = reservations,
                        error = null
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Unable to read reservation data."
                    )
                }
            }

            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Unable to load reservations."
                )
            }
        }
    }

    fun cancelReservation(
        idToken: String,
        reservationId: String
    ) {
        if (idToken.isBlank()) {
            _uiState.value = _uiState.value.copy(
                cancellationError = "Authentication token is missing."
            )
            return
        }

        if (_uiState.value.cancellingReservationId != null) return

        _uiState.value = _uiState.value.copy(
            cancellingReservationId = reservationId,
            cancellationError = null,
            cancellationSuccess = null
        )

        SeatSpotApi.cancelReservation(idToken, reservationId) { result ->
            result.onSuccess {
                // Update the local list immediately after backend confirmation.
                val updatedReservations = _uiState.value.reservations.map { reservation ->
                    if (reservation.reservationId == reservationId) {
                        reservation.copy(status = "CANCELLED")
                    } else {
                        reservation
                    }
                }

                _uiState.value = _uiState.value.copy(
                    reservations = updatedReservations,
                    cancellingReservationId = null,
                    cancellationSuccess = "Booking cancelled successfully."
                )

                // Fetch the latest server data.
                loadReservations(idToken)
            }

            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    cancellingReservationId = null,
                    cancellationError = error.message
                        ?: "Unable to cancel this booking."
                )
            }
        }
    }

    fun clearCancellationMessages() {
        _uiState.value = _uiState.value.copy(
            cancellationError = null,
            cancellationSuccess = null
        )
    }
}
