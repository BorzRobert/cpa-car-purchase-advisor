package ro.cpa.advisor.api;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ro.cpa.advisor.config.RequestIdFilter;
import ro.cpa.advisor.domain.AdvisorNotConfiguredException;

/**
 * Toate erorile API ies ca RFC 9457 `ProblemDetail`, cu acelasi format, ca frontend-ul sa aiba un singur mod de a le citi.
 * Extinde handler-ul Spring ca sa pastreze tratarea standard (400, 404, 405, 415...) si adauga doar ce e specific CPA.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AdvisorNotConfiguredException.class)
    ResponseEntity<Object> handleAdvisorNotConfigured(AdvisorNotConfiguredException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setType(URI.create("urn:cpa:problem:advisor-not-configured"));
        problem.setTitle("Advisor not configured");
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    // Plasa de siguranta: clientul primeste un mesaj generic; detaliile (care pot contine date interne) raman doar in log.
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.");
        problem.setType(URI.create("urn:cpa:problem:internal-error"));
        problem.setTitle("Internal server error");
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            String requestId = MDC.get(RequestIdFilter.MDC_KEY);
            if (requestId != null) {
                problem.setProperty("requestId", requestId);
            }
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }
}
