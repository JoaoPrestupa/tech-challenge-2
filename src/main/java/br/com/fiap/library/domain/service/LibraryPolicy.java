package br.com.fiap.library.domain.service;

import br.com.fiap.library.domain.exception.InvalidDataException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

/** Regras configuráveis da biblioteca. Os prazos seguem o calendário do fuso de negócio. */
public record LibraryPolicy(int loanDays, int maxActiveLoans, Duration reservationHold, ZoneId zone) {
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);

    public LibraryPolicy {
        if (loanDays < 1 || maxActiveLoans < 1 || reservationHold == null || reservationHold.isNegative()
                || reservationHold.isZero() || zone == null) {
            throw new InvalidDataException("Política da biblioteca inválida");
        }
    }

    /** O prazo termina no fim do dia local, sem depender do horário em que o empréstimo foi feito. */
    public Instant dueDate(Instant borrowedAt) {
        return borrowedAt.atZone(zone).toLocalDate().plusDays(loanDays).atTime(END_OF_DAY).atZone(zone).toInstant();
    }

    public Instant holdUntil(Instant readyAt) {
        return readyAt.plus(reservationHold);
    }
}
