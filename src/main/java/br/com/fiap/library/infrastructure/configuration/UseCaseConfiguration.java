package br.com.fiap.library.infrastructure.configuration;

import br.com.fiap.library.application.port.in.Authentication;
import br.com.fiap.library.application.port.in.BookManagement;
import br.com.fiap.library.application.port.in.LoanManagement;
import br.com.fiap.library.application.port.in.ReportQueries;
import br.com.fiap.library.application.port.in.ReservationManagement;
import br.com.fiap.library.application.port.in.UserManagement;
import br.com.fiap.library.application.port.out.AccessTokenIssuer;
import br.com.fiap.library.application.port.out.BookCopyRepository;
import br.com.fiap.library.application.port.out.BookRepository;
import br.com.fiap.library.application.port.out.LoanRepository;
import br.com.fiap.library.application.port.out.OpaqueTokenGenerator;
import br.com.fiap.library.application.port.out.PasswordHasher;
import br.com.fiap.library.application.port.out.RefreshSessionRepository;
import br.com.fiap.library.application.port.out.ReservationRepository;
import br.com.fiap.library.application.port.out.UnitOfWork;
import br.com.fiap.library.application.port.out.UserRepository;
import br.com.fiap.library.application.usecase.AuthenticationService;
import br.com.fiap.library.application.usecase.BookService;
import br.com.fiap.library.application.usecase.LoanService;
import br.com.fiap.library.application.usecase.ReportService;
import br.com.fiap.library.application.usecase.ReservationQueue;
import br.com.fiap.library.application.usecase.ReservationService;
import br.com.fiap.library.application.usecase.UserService;
import br.com.fiap.library.domain.service.LibraryPolicy;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfiguration {

    @Bean
    Clock clock(LibraryProperties properties) {
        return Clock.system(ZoneId.of(properties.policy().timezone()));
    }

    @Bean
    LibraryPolicy libraryPolicy(LibraryProperties properties) {
        LibraryProperties.Policy policy = properties.policy();
        return new LibraryPolicy(policy.loanDays(), policy.maxActiveLoans(), policy.reservationHold(),
                ZoneId.of(policy.timezone()));
    }

    @Bean
    ReservationQueue reservationQueue(ReservationRepository reservations, BookCopyRepository copies,
                                      LibraryPolicy policy) {
        return new ReservationQueue(reservations, copies, policy);
    }

    @Bean
    BookManagement bookManagement(BookRepository books, BookCopyRepository copies, LoanRepository loans,
                                  ReservationRepository reservations, ReservationQueue queue,
                                  UnitOfWork unitOfWork, Clock clock) {
        return new BookService(books, copies, loans, reservations, queue, unitOfWork, clock);
    }

    @Bean
    UserManagement userManagement(UserRepository users, LoanRepository loans, ReservationRepository reservations,
                                  RefreshSessionRepository sessions, ReservationQueue queue, PasswordHasher hasher,
                                  UnitOfWork unitOfWork, Clock clock) {
        return new UserService(users, loans, reservations, sessions, queue, hasher, unitOfWork, clock);
    }

    @Bean
    Authentication authentication(UserRepository users, UserManagement userManagement,
                                  RefreshSessionRepository sessions, PasswordHasher hasher,
                                  AccessTokenIssuer accessTokens, OpaqueTokenGenerator refreshTokens,
                                  UnitOfWork unitOfWork, Clock clock, LibraryProperties properties) {
        return new AuthenticationService(users, userManagement, sessions, hasher, accessTokens, refreshTokens,
                unitOfWork, clock, properties.jwt().refreshTtl());
    }

    @Bean
    LoanManagement loanManagement(LoanRepository loans, BookCopyRepository copies, UserRepository users,
                                  ReservationRepository reservations, ReservationQueue queue, LibraryPolicy policy,
                                  UnitOfWork unitOfWork, Clock clock) {
        return new LoanService(loans, copies, users, reservations, queue, policy, unitOfWork, clock);
    }

    @Bean
    ReservationManagement reservationManagement(ReservationRepository reservations, BookRepository books,
                                                BookCopyRepository copies, UserRepository users,
                                                ReservationQueue queue, UnitOfWork unitOfWork, Clock clock) {
        return new ReservationService(reservations, books, copies, users, queue, unitOfWork, clock);
    }

    @Bean
    ReportQueries reportQueries(LoanRepository loans) {
        return new ReportService(loans);
    }
}
