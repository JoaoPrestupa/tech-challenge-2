package br.com.fiap.library.adapter.in.web;

import br.com.fiap.library.domain.exception.AuthenticationFailedException;
import br.com.fiap.library.domain.exception.BusinessRuleException;
import br.com.fiap.library.domain.exception.ConflictException;
import br.com.fiap.library.domain.exception.DomainException;
import br.com.fiap.library.domain.exception.ForbiddenException;
import br.com.fiap.library.domain.exception.InvalidDataException;
import br.com.fiap.library.domain.exception.NotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Respostas de erro no formato RFC 9457, sem expor detalhes internos ao cliente. */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException e) {
        return ProblemDetail.forStatusAndDetail(statusOf(e), e.getMessage());
    }

    /** Última barreira: uma corrida barrada por constraint do banco vira conflito, e não erro 500. */
    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class})
    ProblemDetail handleConcurrency(Exception e) {
        log.debug("Conflito de dados", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "A operação conflita com o estado atual dos dados. Consulte e tente novamente.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e) {
        log.error("Erro inesperado", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Dados inválidos na requisição");
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .toList();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    private static HttpStatus statusOf(DomainException e) {
        return switch (e) {
            case NotFoundException ignored -> HttpStatus.NOT_FOUND;
            case ConflictException ignored -> HttpStatus.CONFLICT;
            case ForbiddenException ignored -> HttpStatus.FORBIDDEN;
            case InvalidDataException ignored -> HttpStatus.BAD_REQUEST;
            case BusinessRuleException ignored -> HttpStatus.UNPROCESSABLE_CONTENT;
            case AuthenticationFailedException ignored -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
