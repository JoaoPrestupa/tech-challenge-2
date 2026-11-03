package br.com.fiap.library.application.model;

import br.com.fiap.library.domain.model.Reservation;

public record ReservationView(Reservation reservation, Long queuePosition) {
}
