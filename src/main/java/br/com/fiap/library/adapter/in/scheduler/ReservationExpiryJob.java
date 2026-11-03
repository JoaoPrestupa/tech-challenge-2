package br.com.fiap.library.adapter.in.scheduler;

import br.com.fiap.library.application.port.in.ReservationManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class ReservationExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);

    private final ReservationManagement reservations;

    ReservationExpiryJob(ReservationManagement reservations) {
        this.reservations = reservations;
    }

    @Scheduled(fixedDelayString = "${library.policy.expiry-check-interval}",
            initialDelayString = "${library.policy.expiry-check-interval}")
    void expireOverdueReservations() {
        int expired = reservations.expireOverdue();
        if (expired > 0) {
            log.info("{} reserva(s) expirada(s)", expired);
        }
    }
}
