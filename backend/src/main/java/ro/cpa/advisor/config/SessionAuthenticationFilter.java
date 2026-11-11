package ro.cpa.advisor.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import ro.cpa.advisor.domain.SessionPrincipal;
import ro.cpa.advisor.service.SessionService;

/**
 * Citeste `Authorization: Bearer <token>` si, daca tokenul e valid, marcheaza cererea ca autentificata. Nu respinge
 * nimic: cererile fara token valid trec mai departe neautentificate, iar regulile de acces din `SecurityConfig` decid
 * (401 doar acolo unde se cere sesiune). Nu e bean: se creeaza in lantul de securitate, ca Boot sa nu-l inregistreze
 * a doua oara ca filtru servlet obisnuit.
 */
class SessionAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SessionService sessions;

    SessionAuthenticationFilter(SessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            Optional<SessionPrincipal> principal = sessions.authenticate(header.substring(BEARER_PREFIX.length()).trim());
            principal.ifPresent(p -> SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(p, null, AuthorityUtils.createAuthorityList("ROLE_VISITOR"))));
        }
        chain.doFilter(request, response);
    }
}
