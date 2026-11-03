package br.com.fiap.library.adapter.in.web;

import br.com.fiap.library.application.model.AuthTokens;
import br.com.fiap.library.application.model.BookView;
import br.com.fiap.library.application.model.LoanDetails;
import br.com.fiap.library.application.model.ReservationView;
import br.com.fiap.library.domain.model.BookCopy;
import br.com.fiap.library.domain.model.CopyStatus;
import br.com.fiap.library.domain.model.LibraryUser;
import br.com.fiap.library.domain.model.Reservation;
import br.com.fiap.library.domain.model.ReservationStatus;
import br.com.fiap.library.domain.model.Role;
import br.com.fiap.library.domain.model.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class Responses {
    record BookResponse(Long id, String title, String author, String isbn, String description, boolean available,
                        long totalCopies, long availableCopies) {
    }

    record CopyResponse(Long id, Long bookId, String assetCode, CopyStatus status) {
    }

    /** Nunca inclui a senha nem o hash. */
    record UserResponse(Long id, String name, String email, UserStatus status, List<Role> roles) {
    }

    record TokenResponse(String accessToken, String tokenType, OffsetDateTime accessTokenExpiresAt,
                         String refreshToken, OffsetDateTime refreshTokenExpiresAt) {
    }

    record LoanResponse(Long id, Long userId, String userName, Long bookCopyId, String assetCode, Long bookId,
                        String bookTitle, OffsetDateTime borrowedAt, OffsetDateTime dueAt, OffsetDateTime returnedAt,
                        boolean active, boolean overdue, Long daysUntilDue) {
    }

    record ReservationResponse(Long id, Long userId, Long bookId, Long bookCopyId, ReservationStatus status,
                               OffsetDateTime createdAt, OffsetDateTime readyUntil, Long queuePosition) {
    }

    private final Clock clock;
    private final ZoneId zone;

    Responses(Clock clock) {
        this.clock = clock;
        this.zone = clock.getZone();
    }

    BookResponse book(BookView view) {
        var book = view.book();
        return new BookResponse(book.id(), book.title(), book.author(), book.isbn(), book.description(),
                view.available(), view.totalCopies(), view.availableCopies());
    }

    CopyResponse copy(BookCopy copy) {
        return new CopyResponse(copy.id(), copy.bookId(), copy.assetCode(), copy.status());
    }

    UserResponse user(LibraryUser user) {
        return new UserResponse(user.id(), user.name(), user.email(), user.status(),
                user.roles().stream().sorted().toList());
    }

    TokenResponse tokens(AuthTokens tokens) {
        return new TokenResponse(tokens.accessToken(), "Bearer", at(tokens.accessTokenExpiresAt()),
                tokens.refreshToken(), at(tokens.refreshTokenExpiresAt()));
    }

    LoanResponse loan(LoanDetails loan) {
        Instant now = clock.instant();
        boolean active = loan.returnedAt() == null;
        Long days = active
                ? ChronoUnit.DAYS.between(now.atZone(zone).toLocalDate(), loan.dueAt().atZone(zone).toLocalDate())
                : null;
        return new LoanResponse(loan.id(), loan.userId(), loan.userName(), loan.bookCopyId(), loan.assetCode(),
                loan.bookId(), loan.bookTitle(), at(loan.borrowedAt()), at(loan.dueAt()), at(loan.returnedAt()),
                active, active && loan.dueAt().isBefore(now), days);
    }

    ReservationResponse reservation(ReservationView view) {
        Reservation r = view.reservation();
        return new ReservationResponse(r.id(), r.userId(), r.bookId(), r.bookCopyId(), r.status(),
                at(r.createdAt()), at(r.readyUntil()), view.queuePosition());
    }

    private OffsetDateTime at(Instant instant) {
        return instant == null ? null : instant.atZone(zone).toOffsetDateTime();
    }
}
