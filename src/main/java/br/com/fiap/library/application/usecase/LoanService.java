package br.com.fiap.library.application.usecase;

import br.com.fiap.library.application.model.Actor;
import br.com.fiap.library.application.model.LoanDetails;
import br.com.fiap.library.application.model.LoanFilter;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.application.port.in.LoanManagement;
import br.com.fiap.library.application.port.out.BookCopyRepository;
import br.com.fiap.library.application.port.out.LoanRepository;
import br.com.fiap.library.application.port.out.ReservationRepository;
import br.com.fiap.library.application.port.out.UnitOfWork;
import br.com.fiap.library.application.port.out.UserRepository;
import br.com.fiap.library.domain.exception.BusinessRuleException;
import br.com.fiap.library.domain.exception.ConflictException;
import br.com.fiap.library.domain.exception.NotFoundException;
import br.com.fiap.library.domain.model.BookCopy;
import br.com.fiap.library.domain.model.CopyStatus;
import br.com.fiap.library.domain.model.LibraryUser;
import br.com.fiap.library.domain.model.Loan;
import br.com.fiap.library.domain.model.Reservation;
import br.com.fiap.library.domain.service.LibraryPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

public class LoanService implements LoanManagement {
    private final LoanRepository loans;
    private final BookCopyRepository copies;
    private final UserRepository users;
    private final ReservationRepository reservations;
    private final ReservationQueue queue;
    private final LibraryPolicy policy;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public LoanService(LoanRepository loans, BookCopyRepository copies, UserRepository users,
                       ReservationRepository reservations, ReservationQueue queue, LibraryPolicy policy,
                       UnitOfWork unitOfWork, Clock clock) {
        this.loans = loans;
        this.copies = copies;
        this.users = users;
        this.reservations = reservations;
        this.queue = queue;
        this.policy = policy;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    /**
     * A linha do usuário é bloqueada para que requisições simultâneas não ultrapassem o limite de
     * empréstimos, e a do exemplar para evitar empréstimo duplicado. O índice único parcial em
     * loans é a última barreira, no próprio banco.
     */
    @Override
    public LoanDetails borrow(Long userId, Long bookCopyId) {
        Long loanId = unitOfWork.inTransaction(() -> {
            LibraryUser user = users.findByIdForUpdate(userId)
                    .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + userId));
            if (!user.isActive()) {
                throw new BusinessRuleException("Usuário inativo não pode pegar livros emprestados");
            }
            if (loans.countActiveByUser(userId) >= policy.maxActiveLoans()) {
                throw new BusinessRuleException("Limite de " + policy.maxActiveLoans() + " empréstimos ativos atingido");
            }
            BookCopy copy = copies.findByIdForUpdate(bookCopyId)
                    .orElseThrow(() -> new NotFoundException("Exemplar não encontrado: " + bookCopyId));

            settleReservation(copy, userId);

            Instant now = clock.instant();
            copies.save(copy.loan());
            return loans.save(Loan.open(userId, bookCopyId, now, policy.dueDate(now))).id();
        });
        return requireDetails(loanId);
    }

    private void settleReservation(BookCopy copy, Long userId) {
        Optional<Reservation> own = reservations.findOpenByUserAndBook(userId, copy.bookId());
        if (copy.status() == CopyStatus.RESERVED
                && own.filter(r -> r.isReady() && copy.id().equals(r.bookCopyId())).isEmpty()) {
            throw new ConflictException("O exemplar " + copy.assetCode() + " está separado para outra reserva");
        }
        if (own.filter(r -> r.isReady() && !copy.id().equals(r.bookCopyId())).isPresent()) {
            throw new ConflictException("O usuário deve retirar o exemplar que já está separado para ele");
        }
        own.ifPresent(reservation -> reservations.save(reservation.fulfill()));
    }

    @Override
    public LoanDetails giveBack(Long loanId) {
        unitOfWork.run(() -> {
            Loan loan = loans.findByIdForUpdate(loanId)
                    .orElseThrow(() -> new NotFoundException("Empréstimo não encontrado: " + loanId));
            Instant now = clock.instant();
            loans.save(loan.close(now));
            BookCopy copy = copies.findByIdForUpdate(loan.bookCopyId())
                    .orElseThrow(() -> new NotFoundException("Exemplar não encontrado: " + loan.bookCopyId()));
            queue.offer(copy, now);
        });
        return requireDetails(loanId);
    }

    @Override
    public LoanDetails get(Actor actor, Long loanId) {
        LoanDetails details = requireDetails(loanId);
        actor.requireSelfOrLibrarian(details.userId());
        return details;
    }

    @Override
    public PageResult<LoanDetails> search(LoanFilter filter, PageQuery page) {
        return loans.search(filter, page);
    }

    @Override
    public PageResult<LoanDetails> historyOf(Actor actor, Long userId, PageQuery page) {
        actor.requireSelfOrLibrarian(userId);
        users.findById(userId).orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + userId));
        return loans.search(new LoanFilter(userId, null), page);
    }

    private LoanDetails requireDetails(Long loanId) {
        return loans.findDetailsById(loanId)
                .orElseThrow(() -> new NotFoundException("Empréstimo não encontrado: " + loanId));
    }
}
