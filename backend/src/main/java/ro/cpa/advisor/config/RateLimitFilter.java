package ro.cpa.advisor.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import ro.cpa.advisor.domain.RateLimitExceededException;
import ro.cpa.advisor.domain.SessionPrincipal;
import ro.cpa.advisor.service.RateLimitService;

/**
 * Aplica limitele de rata pe `/api/v1/**`, dupa autentificare (ca sa stie sesiunea). Fiind in afara MVC, un 429 nu
 * trece prin controllere; il predam resolver-ului de exceptii ca sa iasa ca acelasi `ProblemDetail` ca toate erorile.
 * Nu e bean din acelasi motiv ca `SessionAuthenticationFilter`.
 */
class RateLimitFilter extends OncePerRequestFilter {

    private static final String API_PREFIX = "/api/v1/";
    private static final String SESSIONS_PATH = "/api/v1/sessions";

    private final RateLimitService rateLimits;
    private final HandlerExceptionResolver exceptionResolver;
    private final int trustedProxyHops;

    RateLimitFilter(RateLimitService rateLimits, HandlerExceptionResolver exceptionResolver, int trustedProxyHops) {
        this.rateLimits = rateLimits;
        this.exceptionResolver = exceptionResolver;
        this.trustedProxyHops = trustedProxyHops;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Health check-urile gazdei si documentatia nu sunt limitate: nu consuma nimic si nu trebuie sa pice niciodata.
        return !request.getRequestURI().startsWith(API_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String ip = ClientIpResolver.resolve(request, trustedProxyHops);
            rateLimits.checkRequest(ip, currentSessionId());
            if (HttpMethod.POST.matches(request.getMethod()) && SESSIONS_PATH.equals(request.getRequestURI())) {
                rateLimits.checkSessionCreation(ip);
            }
        } catch (RateLimitExceededException ex) {
            exceptionResolver.resolveException(request, response, null, ex);
            return;
        }
        chain.doFilter(request, response);
    }

    private static UUID currentSessionId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof SessionPrincipal principal ? principal.sessionId() : null;
    }
}
