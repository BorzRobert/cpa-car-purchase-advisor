package ro.cpa.advisor.api;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ro.cpa.advisor.config.RequestIdFilter;
import ro.cpa.advisor.domain.AdvisorNotConfiguredException;
import ro.cpa.advisor.domain.ConflictException;
import ro.cpa.advisor.domain.ForbiddenException;
import ro.cpa.advisor.domain.InvalidInputException;
import ro.cpa.advisor.domain.LimitReachedException;
import ro.cpa.advisor.domain.NotFoundException;
import ro.cpa.advisor.domain.RateLimitExceededException;
import ro.cpa.advisor.domain.UnauthorizedException;

/**
 * Toate erorile API ies ca RFC 9457 `ProblemDetail`, cu acelasi format, ca frontend-ul sa aiba un singur mod de a le citi.
 * Extinde handler-ul Spring ca sa pastreze tratarea standard (400, 404, 405, 415...) si adauga doar ce e specific CPA.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String TYPE_PREFIX = "urn:cpa:problem:";

    @ExceptionHandler(AdvisorNotConfiguredException.class)
    ResponseEntity<Object> handleAdvisorNotConfigured(AdvisorNotConfiguredException ex, WebRequest request) {
        return respond(ex, request, HttpStatus.SERVICE_UNAVAILABLE, "advisor-not-configured", "Advisor not configured", ex.getMessage(), new HttpHeaders());
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<Object> handleNotFound(NotFoundException ex, WebRequest request) {
        return respond(ex, request, HttpStatus.NOT_FOUND, "not-found", "Not found", ex.getMessage(), new HttpHeaders());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<Object> handleConflict(ConflictException ex, WebRequest request) {
        return respond(ex, request, HttpStatus.CONFLICT, ex.code(), "Conflict", ex.getMessage(), new HttpHeaders());
    }

    @ExceptionHandler(LimitReachedException.class)
    ResponseEntity<Object> handleLimitReached(LimitReachedException ex, WebRequest request) {
        return respond(ex, request, HttpStatus.CONFLICT, "limit-reached", "Limit reached", ex.getMessage(), new HttpHeaders());
    }

    // O constrangere unica incalcata de doua cereri simultane (verificarea din service nu e atomica) ajunge aici.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return respond(ex, request, HttpStatus.CONFLICT, "conflict", "Conflict", "The request conflicts with the current state.", new HttpHeaders());
    }

    @ExceptionHandler(InvalidInputException.class)
    ResponseEntity<Object> handleInvalidInput(InvalidInputException ex, WebRequest request) {
        return respond(ex, request, HttpStatus.BAD_REQUEST, "invalid-input", "Invalid input", ex.getMessage(), new HttpHeaders());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<Object> handleRateLimited(RateLimitExceededException ex, WebRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds()));
        return respond(ex, request, HttpStatus.TOO_MANY_REQUESTS, ex.code(), "Too many requests", ex.getMessage(), headers);
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<Object> handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return respond(ex, request, HttpStatus.UNAUTHORIZED, "unauthorized", "Unauthorized", ex.getMessage(), headers);
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<Object> handleForbidden(ForbiddenException ex, WebRequest request) {
        return respond(ex, request, HttpStatus.FORBIDDEN, "forbidden", "Forbidden", ex.getMessage(), new HttpHeaders());
    }

    // Plasa de siguranta: clientul primeste un mesaj generic; detaliile (care pot contine date interne) raman doar in log.
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        return respond(ex, request, HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal server error", "Unexpected server error.", new HttpHeaders());
    }

    // Validarea de format (Bean Validation pe corpul cererii): lista campurilor gresite, ca UI-ul sa le poata marca.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation-error", "Validation error", "The request body is not valid.");
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", String.valueOf(e.getDefaultMessage())))
                .toList());
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    private ResponseEntity<Object> respond(
            Exception ex, WebRequest request, HttpStatus status, String code, String title, String detail, HttpHeaders headers) {
        return handleExceptionInternal(ex, problem(status, code, title, detail), headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String code, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_PREFIX + code));
        problem.setTitle(title);
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        // Unele erori standard (de exemplu cale inexistenta) ajung aici fara corp; il luam din exceptie ca sa primeasca si ele `code`.
        if (body == null && ex instanceof ErrorResponse errorResponse) {
            body = errorResponse.getBody();
        }
        if (body instanceof ProblemDetail problem) {
            String requestId = MDC.get(RequestIdFilter.MDC_KEY);
            if (requestId != null) {
                problem.setProperty("requestId", requestId);
            }
            problem.setProperty("code", codeOf(problem, statusCode));
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    // `code` este identificatorul stabil pe care UI-ul il traduce in text; pentru erorile standard Spring il deducem din status.
    private static String codeOf(ProblemDetail problem, HttpStatusCode statusCode) {
        // Pentru erorile standard Spring tipul poate lipsi (null) sau fi `about:blank`.
        String type = problem.getType() == null ? "about:blank" : problem.getType().toString();
        if (type.startsWith(TYPE_PREFIX)) {
            return type.substring(TYPE_PREFIX.length());
        }
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status == null ? "error" : status.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
